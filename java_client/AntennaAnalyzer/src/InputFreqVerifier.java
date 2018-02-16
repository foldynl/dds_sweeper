import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

public class InputFreqVerifier extends InputVerifier {

	@Override
	public boolean shouldYieldFocus(JComponent input) {
		boolean valid = verify(input);

        if (valid) {
            return true;
        } else {
            return false;
        }
    }
	
	@Override
	public boolean verify(JComponent input) {
		String text = ((JTextField) input).getText();
		int value;
	    try {
	         value = Integer.parseInt(text);
	             
	        } catch (NumberFormatException e) {
	        	JOptionPane.showMessageDialog( 
	    	              null, "Not a number", "Invalid input", JOptionPane.ERROR_MESSAGE);
	            return false;
	        }
	    if (value >= 1 && value <= 40000)
	    {
	    	return true;
	    }
	    else
	    {
	    	JOptionPane.showMessageDialog( 
  	              null, "Number must be between 1 and 40000", "Invalid input", JOptionPane.ERROR_MESSAGE);
	    	return false;
	    }
	}

}
