package cn.jugame.http;

import java.io.ByteArrayOutputStream;

/**
 * TODO 解析form-data结构的参数。
 * @author zimT_T
 *
 */
public class FormDataParser extends AParser{
	private String boundary;
	private String begBoundary;
	private String endBoundary;
	public FormDataParser(String boundary, byte[] data){
		this.boundary = boundary;
		append_bytes(data);
		
		//form-data的标识
		this.begBoundary = "--" + boundary;
		this.endBoundary = "--" + boundary + "--";
	}
	
	public void parse(){
		String line = null;
		while((line = read_line()) != null){
			if(begBoundary.equals(line)){
				//解析一个参数
//				parseHeader();
//				parseParameter();
			}
			
			//解析到结尾了
			if(endBoundary.equals(line))
				break;
		}
	}
	
	private void parseHeader(){
		
	}
}
