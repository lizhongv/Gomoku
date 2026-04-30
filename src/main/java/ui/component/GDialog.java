package ui.component;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import ui.Theme;
import ui.design.Tokens;

public class GDialog extends JDialog {
    protected final JPanel content;

    public GDialog(Window owner, String title, boolean modal) {
        super(owner, title, modal ? ModalityType.APPLICATION_MODAL : ModalityType.MODELESS);
        Tokens t = Theme.tokens();
        content = new JPanel(new BorderLayout());
        content.setBackground(t.bgPrimary);
        content.setBorder(BorderFactory.createEmptyBorder(t.space5, t.space5, t.space5, t.space5));
        setContentPane(content);
        getRootPane().registerKeyboardAction(
            e -> dispose(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }
}
