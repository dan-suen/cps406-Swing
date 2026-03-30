import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        // Ensure GUI creation and updates are done on the Event Dispatch Thread
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                createAndShowGUI();
            }
        });
    }

    private static void createAndShowGUI() {
        // Create and set up the main window
        JFrame frame = new JFrame("Java Swing Skeleton");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Set the size of the window
        frame.setSize(400, 300);

        // Create a simple label and add it to the window
        JLabel label = new JLabel("Hello, World! This is a Swing Skeleton.", JLabel.CENTER);
        frame.getContentPane().add(label);

        // Center the window on the screen
        frame.setLocationRelativeTo(null); 
        
        // Display the window
        frame.setVisible(true);
    }
}
