package cn.jugame.mt;

public class ProtocalParseException extends RuntimeException{
	private static final long serialVersionUID = 1L;
	
	public ProtocalParseException(){
		super();
	}
	
	public ProtocalParseException(String msg){
		super(msg);
	}
}
