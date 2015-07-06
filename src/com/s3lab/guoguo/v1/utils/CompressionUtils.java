package com.s3lab.guoguo.v1.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import android.util.Log;

public class CompressionUtils {
	public static byte[] compress(byte[] data) throws IOException {
		Deflater deflater = new Deflater();
		deflater.setInput(data);

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(
				data.length);

		deflater.finish();
		byte[] buffer = new byte[1024];
		while (!deflater.finished()) {
			int count = deflater.deflate(buffer); // returns the generated
													// code... index
			outputStream.write(buffer, 0, count);
		}
		outputStream.close();
		byte[] output = outputStream.toByteArray();

		deflater.end();

		Log.i("compression ", "Original: " + data.length / 1024 + " Kb");
		Log.i("compression ", "Compressed: " + output.length / 1024 + " Kb");
		return output;
	}

	public static byte[] decompress(byte[] data) throws IOException,
			DataFormatException {
		Inflater inflater = new Inflater();
		inflater.setInput(data);

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(
				data.length);
		byte[] buffer = new byte[1024];
		while (!inflater.finished()) {
			int count = inflater.inflate(buffer);
			outputStream.write(buffer, 0, count);
		}
		outputStream.close();
		byte[] output = outputStream.toByteArray();

		inflater.end();

		Log.i("compression ", "Original: " + data.length);
		Log.i("compression ", "Uncompressed: " + output.length);
		return output;
	}
}