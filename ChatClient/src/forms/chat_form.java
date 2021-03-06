package forms;

import javax.swing.*;
import javax.swing.text.*;
import nodes.*;
import src.ConnectionOutClient;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class chat_form extends JFrame{
    private JButton button_leave;
    private JPanel panel_chat_from;
    private JTextPane textPane_chat;
    private JTextArea textArea_message;
    private JButton button_create_task;
    private JButton button_members;
    private JButton button_send;
    private JButton button_show_task;
    private JLabel label_conv_title;
    private Document doc;
    private JFrame frame;

    public chat_form(ArrayList<JFrame> FramesList, ConnectionOutClient out, MessageNode info, MessageNode[] messages) {
        setSize(400, 500);
        String conv_title = info.text1;
        String conv_link = info.text2;
        this.setVisible(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE );
        this.getContentPane().add(panel_chat_from);
        this.setName("chat_form"+conv_link);
        label_conv_title.setText(conv_title);
        FramesList.add(this);
        JScrollPane jsp = new JScrollPane(textPane_chat);
        doc = textPane_chat.getDocument();
        updateContent(messages);
        frame = this;

        button_send.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (textArea_message.getText().equals("")){
                    return;
                }
                MessageObject mo = new MessageObject();
                mo.code = 71;
                mo.info.text1 = conv_link;
                mo.info.text2 = textArea_message.getText();
                out.SendMessage(mo);
                textArea_message.setText("");
            }
        });

        button_leave.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MessageObject mo = new MessageObject();
                mo.code = 73;
                mo.info.text1 = conv_link;
                out.SendMessage(mo);
                FramesList.remove(this);
                FormsHelper.get_frame(e).dispose();
            }
        });

        button_members.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MessageObject mo = new MessageObject();
                mo.code = 74;
                mo.info.text1 = conv_link;
                out.SendMessage(mo);
            }
        });

        button_create_task.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(FormsHelper.get_frame(e), "Not implemented.");
            }
        });

        button_show_task.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(FormsHelper.get_frame(e), "Not implemented.");
            }
        });

        this.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                FramesList.remove(frame);
                frame = null;
            }
        });

    }

    public void updateContent(MessageNode[] messageNodes) {
        if (messageNodes == null) return;
        for (MessageNode mn: messageNodes) {
            try{
                doc.insertString(doc.getLength(), mn.text1 + " : " + mn.text2 + "\n", null);
                textPane_chat.updateUI();
            } catch (BadLocationException e){
            }
        }
    }
} // +-
