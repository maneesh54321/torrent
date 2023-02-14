package com.lib.torrent.utils;

public class BinaryDataUtils {
    public static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte aByte : bytes) {
            result.append(String.format("%02x", aByte));
//             upper case
//            result.append(String.format("%02X ", aByte));
        }
        return result.toString();
    }

    public static String hex(byte[] bytes, int offset, int length) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i<length) {
            result.append(String.format("%02x", bytes[i+offset]));
            i++;
        }
        return result.toString();
    }

    public static String toBinaryString(byte[] bytes, int offset, int length){
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i<length) {
            result.append(String.format("%8s",
                    Integer.toBinaryString(bytes[i+offset] & 0xff)).replace(' ', '0'));
            i++;
        }
        return result.toString();
    }

    public static byte[] decodeHexString(String s) {
        byte[] ans = new byte[s.length() / 2];

        System.out.println("Hex String : " + s);

        for (int i = 0; i < ans.length; i++) {
            int index = i * 2;

            // Using parseInt() method of Integer class
            int val = Integer.parseInt(s.substring(index, index + 2), 16);
            ans[i] = (byte) val;
        }

        // Printing the required Byte Array
        System.out.print("Byte Array : ");
        for (byte an : ans) {
            System.out.printf("%02X ", an);
        }

        return ans;
    }


}
