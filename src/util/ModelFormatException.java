package util;

public class ModelFormatException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1759295131170457220L;
	
	private int position;
	
	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public ModelFormatException() {
		super();
	}
	
	public ModelFormatException(int p, String e) {
		super(e);
		this.position=p;
	}
	
	@Override
	public String getMessage() {
		return super.getMessage();
		
	}

}
