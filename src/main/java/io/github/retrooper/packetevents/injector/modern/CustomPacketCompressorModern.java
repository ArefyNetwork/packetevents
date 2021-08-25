/*
 * This file is part of packetevents - https://github.com/retrooper/packetevents
 * Copyright (C) 2021 retrooper and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.retrooper.packetevents.injector.modern;

import io.github.retrooper.packetevents.utils.netty.bytebuf.ByteBufModern;
import io.github.retrooper.packetevents.wrapper.PacketWrapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.zip.Deflater;

public class CustomPacketCompressorModern {
    private static final byte[] COMPRESSED_DATA = new byte[8192];
    private static final Deflater DEFLATER = new Deflater();
    private static final int THRESHOLD = 256;

    public static ByteBuf compress(ChannelHandlerContext ctx, ByteBuf byteBuf) {
        ByteBuf output = ctx.alloc().buffer();
        if (!(ctx.pipeline().get("compress") instanceof MessageToByteEncoder)) {
            //ViaRewind replaced the compressor with an empty handler, so we can just skip the compression process.
            return output.writeBytes(byteBuf);
        }
        int dataLength = byteBuf.readableBytes();
        //TODO generate()
        PacketWrapper outputWrapper = PacketWrapper.createUniversalPacketWrapper(new ByteBufModern(output));
        if (dataLength < THRESHOLD) {
            //Set data length to 0
            outputWrapper.writeVarInt(0);
            output.writeBytes(byteBuf);
        } else {
            byte[] decompressedData = new byte[dataLength];
            byteBuf.readBytes(decompressedData);
            outputWrapper.writeVarInt(decompressedData.length);
            DEFLATER.setInput(decompressedData, 0, dataLength);
            DEFLATER.finish();

            while (!DEFLATER.finished()) {
                int var6 = DEFLATER.deflate(COMPRESSED_DATA);
                output.writeBytes(COMPRESSED_DATA, 0, var6);
            }

            DEFLATER.reset();
        }
        return output;
    }
}