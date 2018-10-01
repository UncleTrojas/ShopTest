package my.shop.email;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class Mail {
	
	private String from;
	private String to;
	private String subject;
	private String content;
	
	public Mail() {
		this.from = "@gmail.com";
	}
	
	

}
