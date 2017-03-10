package org.kevoree.library.java.core.console;

import org.kevoree.library.ToyConsole;
import org.kevoree.log.Log;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Created with IntelliJ IDEA.
 * User: gregory.nain
 * Date: 02/12/2013
 * Time: 10:59
 */
public class ConsolePanel extends JPanel {

    private static final String INITIAL_MESSAGE = "Type your text here";

    private int frameWidth = 300;
    private int frameHeight = 600;


    private JTextPane screen;
    private JTextArea inputTextField;
    private JButton send;
    private JPanel bottomPanel;

    private ToyConsole attachedComponent;


    public ConsolePanel(ToyConsole attachedComponent) {

        this.attachedComponent = attachedComponent;

        initGraphicalComponents();
        layoutGraphicalComponents();


    }

    private void initGraphicalComponents() {
        initSendButton();
        initInputTextfield();
        initScreen();

        bottomPanel = new JPanel();

    }

    private void initSendButton() {
        send = new JButton("Send");
        send.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (inputTextField.getText().length() > 1) {
                    appendOutgoing(inputTextField.getText());
                    attachedComponent.textTypedLocally(inputTextField.getText());
                }

            }
        });
    }

    private void initInputTextfield() {
        inputTextField = new JTextArea();
        inputTextField.setText(INITIAL_MESSAGE);
        inputTextField.setFocusable(true);
        inputTextField.setRequestFocusEnabled(true);
        inputTextField.requestFocus();
        inputTextField.setCaretPosition(0);
        inputTextField.setSelectionStart(0);
        inputTextField.setSelectionEnd(INITIAL_MESSAGE.length());

        inputTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER ) {
                    if (e.isControlDown()) {
                        inputTextField.append("\n");
                    } else {
                        if (inputTextField.getText().length() > 1) {
                            appendOutgoing(inputTextField.getText());
                            attachedComponent.textTypedLocally(inputTextField.getText());
                        }
                        inputTextField.setText("");
                    }
                }
            }
        });
    }

    private void initScreen() {
        screen = new JTextPane();
        screen.setFocusable(false);
        screen.setEditable(false);

        StyledDocument doc = screen.getStyledDocument();
        Style def = StyleContext.getDefaultStyleContext().
                getStyle(StyleContext.DEFAULT_STYLE);
        Style system = doc.addStyle("system", def);
        StyleConstants.setForeground(system, Color.GRAY);

        Style incoming = doc.addStyle("incoming", def);
        StyleConstants.setForeground(incoming, Color.BLUE);

        Style outgoing = doc.addStyle("outgoing", def);
        StyleConstants.setForeground(outgoing, Color.GREEN);
    }

    private void layoutGraphicalComponents() {
        setPreferredSize(new Dimension(frameWidth, frameHeight));
        setLayout(new BorderLayout());

        bottomPanel.setLayout(new BorderLayout());
        bottomPanel.add(inputTextField, BorderLayout.CENTER);
        bottomPanel.add(send, BorderLayout.EAST);

        add(new JScrollPane(screen), BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
        setVisible(true);

    }


    public void appendSystem(String text) {
        try {
            StyledDocument doc = screen.getStyledDocument();
            doc.insertString(doc.getLength(), formatForPrint(text), doc.getStyle("system"));
        } catch (BadLocationException ex) {
//                ex.printStackTrace();
            Log.error("Error while trying to append system message in the " + this.getName(), ex);
        }
    }

    public void appendIncomming(String text) {
        try {
            StyledDocument doc = screen.getStyledDocument();
            doc.insertString(doc.getLength(), formatForPrint(text), doc.getStyle("incoming"));
            screen.setCaretPosition(doc.getLength());
        } catch (BadLocationException ex) {
//                ex.printStackTrace();
            Log.error("Error while trying to append incoming message in the " + this.getName(), ex);
            //getLoggerLocal().error(ex.getClass().getSimpleName() + " occured while trying to append text in the terminal.", ex);
        }
    }

    public void appendOutgoing(String text) {
        try {
            StyledDocument doc = screen.getStyledDocument();
            doc.insertString(doc.getLength(), ">" + formatForPrint(text), doc.getStyle("outgoing"));
        } catch (BadLocationException ex) {
//                ex.printStackTrace();
            Log.error("Error while trying to append local message in the " + this.getName(), ex);
            //getLoggerLocal().error(ex.getClass().getSimpleName() + " occured while trying to append text in the terminal.", ex);
        }
    }

    private String formatForPrint(String text) {
        return (text.endsWith("\n") ? text : text + "\n");
    }
}
