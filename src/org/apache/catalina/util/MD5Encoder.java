package org.apache.catalina.util;

public final class MD5Encoder {

	// ----------------------------------------------------- Instance Variables

	private static final char[] hexadecimal = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd',
			'e', 'f' };

	// --------------------------------------------------------- Public Methods

	/**
	 * Encodes the 128 bit (16 bytes) MD5 into a 32 character String.
	 *
	 * @param binaryData
	 *            Array containing the digest
	 * @return Encoded MD5, or null if encoding failed
	 */
	public String encode(byte[] binaryData) {

		if (binaryData.length != 16)
			return null;

		char[] buffer = new char[32];

		for (int i = 0; i < 16; i++) {
			int low = (int) (binaryData[i] & 0x0f);
			int high = (int) ((binaryData[i] & 0xf0) >> 4);
			buffer[i * 2] = hexadecimal[high];
			buffer[i * 2 + 1] = hexadecimal[low];
		}

		return new String(buffer);

	}

}
