package cn.jugame.fs;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class FileAccessor {
	private RandomAccessFile raFile;
	private File file;
	public FileAccessor(File file){
		this.file = file;
		try{
			raFile = new RandomAccessFile(file, "rw");
		}catch(Exception e){
			close();
		}
	}
	
	public boolean seek(long pos){
		try{
			raFile.seek(pos);
			return true;
		}catch(Exception e){
			return false;
		}
	}
	
	public boolean seekEnd(){
		try{
			raFile.seek(raFile.length());
			return true;
		}catch(Exception e){
			return false;
		}
	}
	
	public boolean seekFront(){
		try{
			raFile.seek(0);
			return true;
		}catch(Exception e){
			return false;
		}
	}
	
	public void close(){
		if(raFile != null){
			try{raFile.close();}catch(Exception e){}
		}
	}
	
	public int read() {
		try {
			return raFile.read();
		} catch (IOException e) {
			
			return -1;
		}
	}

	public int read(byte[] b, int off, int len)  {
		try {
			return raFile.read(b, off, len);
		} catch (IOException e) {
			
			return -1;
		}
	}

	public int read(byte[] b) {
		try {
			return raFile.read(b);
		} catch (IOException e) {
			return -1;
		}
	}

	public boolean readFully(byte[] b) {
		try {
			raFile.readFully(b);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	public boolean readFully(byte[] b, int off, int len) {
		try {
			raFile.readFully(b, off, len);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	public int skipBytes(int n) {
		try {
			return raFile.skipBytes(n);
		} catch (IOException e) {
			return -1;
		}
	}

	public boolean write(int b) {
		try {
			raFile.write(b);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	public boolean write(byte[] b) {
		try {
			raFile.write(b);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	public boolean write(byte[] b, int off, int len) {
		try {
			raFile.write(b, off, len);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	public long length() {
		try {
			return raFile.length();
		} catch (IOException e) {
			return -1;
		}
	}

	public byte readByte() {
		try {
			return raFile.readByte();
		} catch (IOException e) {
			return -1;
		}
	}

	public final int readInt() {
		try {
			return raFile.readInt();
		} catch (IOException e) {
			return -1;
		}
	}

	public final long readLong() {
		try {
			return raFile.readLong();
		} catch (IOException e) {
			return -1;
		}
	}

	public final String readLine() {
		try {
			return raFile.readLine();
		} catch (IOException e) {
			return null;
		}
	}

	public final String readUTF() {
		try {
			return raFile.readUTF();
		} catch (IOException e) {
			return null;
		}
	}

	public boolean writeBoolean(boolean v) {
		try {
			raFile.writeBoolean(v);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	public boolean writeByte(int v) {
		try {
			raFile.writeByte(v);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	public boolean writeShort(int v) {
		try {
			raFile.writeShort(v);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	public boolean writeChar(int v) {
		try {
			raFile.writeChar(v);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	public boolean writeInt(int v) {
		try {
			raFile.writeInt(v);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	public boolean writeLong(long v) {
		try {
			raFile.writeLong(v);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	public boolean writeBytes(String s) {
		try {
			raFile.writeBytes(s);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	public boolean writeChars(String s) {
		try {
			raFile.writeChars(s);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	public boolean writeUTF(String str) {
		try {
			raFile.writeUTF(str);
			return true;
		} catch (IOException e) {
			return false;
		}
	}
	
}
