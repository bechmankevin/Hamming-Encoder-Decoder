/*
Kevin Bechman
Hamming Code Error Correction Program
The general purpose of this program is to simulate an environment where data is encoded, corrupted, then decoded using the
Hamming code (8,4) error correction technique.  As this program is now, each byte that is decoded can have either one or two bits flipped
(if any are flipped at all).  In the case of one bit, the bit is detected and flipped back to its correct value.  In the case of
two bits, no error correction is performed and a message prints out in the console.

The most significant actions in the program are encoding and decoding.  However, there are a few extra methods that I separated out, 
just to make things easier to work with.  I separated the error-correction into its own method "checkAndCorrect()", which is performed
on each byte in the "decode()" method.

The path to the text file to be encoded is taken as an argument in the console.
E.g. java Hamming data.txt
*/
import java.io.FileInputStream;
import java.io.*;
public class Hamming {
	public static void main(String[]args) throws IOException {
		File data = new File(args[0]);
		File encoded = encode(data);
		decode(encoded);
		
	}
	
	//Hamming(8, 4) codewords with info bit as index # and codeword as value
	final static int[] codewords = {0, 30, 45, 51, 75, 85, 102, 120, 135, 153, 170, 180, 204, 210, 225, 255};
	
	final static int BYTE1MASK = 0xF0;	//1111 0000   Copies most significant four bits of byte, 0's out the rest.
	final static int BYTE2MASK = 0x0F;	//0000 1111   Copies least significant four bits, rest same as above.
	
	// Inputs each byte from the given data file, swaps the bits from the left and right halves, then writes it to a new text file
	static File encode(File file) throws IOException {
		File encoded = new File("encoded.txt");
		int b, b1, b2;
		FileInputStream fin = new FileInputStream(file);
		FileOutputStream fout = new FileOutputStream(encoded);
		
		while(fin.available() > 0) {
			b = fin.read();
			b1 = (BYTE1MASK & b)>>4;	//AND mask applied, shift bits to occupy rightmost 4 bit positions
			b2 = (BYTE2MASK & b);		//AND mask applied, bits already in correct position
			fout.write(codewords[b1]);
			fout.write(codewords[b2]);
		}
		fin.close();
		fout.close();
		return encoded;
	}
	
	//This method houses the IO overhead and organization of the codewords into info bytes after error correction
	static void decode(File encoded) throws IOException {
		File decoded = new File("decoded.txt");
		FileInputStream fin = new FileInputStream(encoded);
		FileOutputStream fout = new FileOutputStream(decoded);
		int resultByte, byte1, byte2;
		
		while(fin.available() > 0) {
			int count = 1;	//d
			byte1 = corruptByte(fin.read());	//A byte is read, sent through corruptByte method, and returned.
			byte1 = checkAndCorrect(byte1);		//The byte is then passed to error correction, then returned (if two bits flipped, still corrupted)						
			byte1 = (byte1 >> 4) << 4;			//Shifted around to place info bits in proper positions
			
			byte2 = corruptByte(fin.read());	//repeat for second byte
			byte2 = checkAndCorrect(byte2);								
			byte2 = byte2 >> 4;
			
			resultByte = byte1 | byte2;	//The two bytes are OR'd together to form the correct info byte (unless two bits were flipped, then still corrupted)
			fout.write(resultByte);
		}
		fin.close();
		fout.close();
	}
	
	
	//Takes a codeword (int), checks it for errors and corrects it (if one bit is flipped) or prints a message to indicate two bits are flipped
	static int checkAndCorrect(int codeword) {
		int[] bit = new int[9];		//bit[1], bit[5] etc.
		int[] infoByte = new int[3];	//infoByte[1] or infoByte[2] to be joined together and outputted to decoded file
		int[] sBit = new int[5]; 	//sBit[1] is s1, sBit[2] is s2, etc.
		
		//Loop through each of the 8 bits of the codeword, ANDing their bit to the respective
		//location in the bits[] array (index 0 is empty, bit 1 = index 1, bit 2 = index 2, etc.)
		//This effectively extracts the bit at each position using the "mask" variable 
		//(1000 0000, then 0100 0000, 0010 0000, etc.)
		for(int i = 1, mask = 128; i <= 8; i++, mask /= 2) {
			bit[i] = codeword & mask;		//If bit at position (i) is 1, stores power of 2; if not, stores 0
			bit[i] = makeBinary(bit[i]);	//Bit converted to 1 or 0 if it has a value or not
		}
		
		
		//Calculate Syndrome bits (uses a lot of addition, but the examples performed the operations this way so it seemed ok to do it here)
		sBit[1] = (bit[4] + bit[5] + bit[6] + bit[7]) % 2;
		sBit[2] = (bit[2] + bit[3] + bit[6] + bit[7]) % 2;
		sBit[3] = (bit[1] + bit[3] + bit[5] + bit[7]) % 2;
		sBit[4] = (bit[1] + bit[2] + bit[3] + bit[4] + bit[5] + bit[6] + bit[7] + bit[8]) % 2;
		
		//Tests to detect which bit is corrupted, if any
		//If s4 is 1, single error
		if (sBit[4] == 1) {
		
			//If s1s2s3 == 000, bit 8 is corrupted
			if(sBit[1] == 0 && sBit[2] == 0 && sBit[3] == 0) {
				bit[8] = bit[8] ^ 1;	//Flip the eighth bit
				System.out.println("Error corrected at bit 8.");
			}
			
			//If s1, s2, or s3 has a 1, s1s2s3 will indicate the corrupted bit location	
			else {
				int corruptedBit = (4 * sBit[1]) + (2 * sBit[2]) + (1 * sBit[3]);
				bit[corruptedBit] = bit[corruptedBit] ^ 1;		//Flip the corrupted bit as indicated by syndrome bits
				System.out.println("Error corrected at bit " + corruptedBit + ".");
			}
		}
		//Else if s4 is 0, either two errors or no errors
		else {		
			if(sBit[1] != 0 || sBit[2] != 0 || sBit[3] != 0)
				System.out.println("Two bits were flipped; can't correct!");
			//Else, no change is needed, because all syndrome bits are 0.
		}
		
		//Once a corrupt bit is flipped back, two or no errors are detected, convert the bit[] array back into an int to return
		codeword = 0;	//Reset codeword to be rebuilt from bit[] array (which has corrected codeword)
		for(int i = 1, pow2 = 128; i <= 8; i++, pow2 /= 2) {
			codeword += bit[i] * pow2;
		}
		
		return codeword;
	}
	
	
	//Receives an int, 1/10 (or whatever Math.random() is multiplied by) chance to flip one or two bits 
	//(mask will always flip at least one, sometimes two), and then returns the int
	static int corruptByte(int b) {
		if((int)(Math.random() * 10) == 0) {			//"mask" can be set to just "1" to only flip a single bit - never two.
			int mask = 1000001;							//If this mask is shifted enough by the shiftVal, it will only flip a single bit
			int shiftVal = (int)(Math.random() * 8);
			mask = mask << shiftVal;
			b = b ^ mask;
			return b;
		}
		//Don't flip; return "byte" without any changes
		return b;
	}
	
	
	//Takes an int and returns a "1" if its value is > 0, else returns "0"
	//This simply prepares each "bit" for the syndrome additions
	static int makeBinary(int n) {
		if(n > 0)
			return 1;
		return 0;
	}	
}







