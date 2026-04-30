package ui.scene.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import net.Communication;
import ui.Theme;
import ui.component.GButton;
import ui.component.GDialog;
import ui.component.GTextField;
import ui.component.GToast;
import ui.design.Tokens;

public class RegisterDialog extends GDialog {
    private final Consumer<String> onSuccess;
    private final GTextField ipField;
    private final GTextField userField;
    private final GTextField emailField;
    private final JPasswordField pwdField;
    private final JPasswordField pwd2Field;

    public RegisterDialog(Window owner, Consumer<String> onSuccess) {
        super(owner, "注册新账号", true);
        this.onSuccess = onSuccess;
        Tokens t = Theme.tokens();

        ipField = new GTextField(16);
        ipField.setText("127.0.0.1");
        userField = new GTextField(16);
        emailField = new GTextField(16);
        pwdField = new JPasswordField(16);
        pwdField.setEchoChar('●');
        pwdField.setFont(t.fontBody);
        pwdField.setBorder(BorderFactory.createEmptyBorder(t.space2, t.space3, t.space2, t.space3));
        pwd2Field = new JPasswordField(16);
        pwd2Field.setEchoChar('●');
        pwd2Field.setFont(t.fontBody);
        pwd2Field.setBorder(BorderFactory.createEmptyBorder(t.space2, t.space3, t.space2, t.space3));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(t.space2, t.space2, t.space2, t.space2);
        c.fill = GridBagConstraints.HORIZONTAL;

        addRow(form, c, 0, "服务器 IP：", ipField);
        addRow(form, c, 1, "用户名：", userField);
        addRow(form, c, 2, "邮  箱：", emailField);
        addRow(form, c, 3, "密  码：", pwdField);
        addRow(form, c, 4, "确认密码：", pwd2Field);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, t.space2, 0));
        buttons.setOpaque(false);
        GButton ok = GButton.primary("注册");
        GButton cancel = GButton.ghost("取消");
        ok.addActionListener(e -> doRegister());
        cancel.addActionListener(e -> dispose());
        buttons.add(ok);
        buttons.add(cancel);

        content.add(form, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);

        setSize(new Dimension(420, 360));
        setLocationRelativeTo(owner);
    }

    private void addRow(JPanel form, GridBagConstraints c, int row, String label, java.awt.Component field) {
        Tokens t = Theme.tokens();
        c.gridx = 0; c.gridy = row; c.weightx = 0;
        JLabel l = new JLabel(label);
        l.setFont(t.fontBody);
        l.setForeground(t.fgDefault);
        form.add(l, c);
        c.gridx = 1; c.weightx = 1;
        form.add(field, c);
    }

    private void doRegister() {
        String ip = ipField.getText().trim();
        String user = userField.getText().trim();
        String email = emailField.getText().trim();
        String p1 = new String(pwdField.getPassword());
        String p2 = new String(pwd2Field.getPassword());
        if (ip.isEmpty() || user.isEmpty() || email.isEmpty() || p1.isEmpty()) {
            GToast.error(this, "请填写所有字段"); return;
        }
        if (user.contains(":")) {
            GToast.error(this, "用户名不能包含冒号"); return;
        }
        if (user.length() < 2 || user.length() > 16) {
            GToast.error(this, "用户名长度需在 2-16 之间"); return;
        }
        if (p1.length() < 4) {
            GToast.error(this, "密码长度至少 4 位"); return;
        }
        if (!p1.equals(p2)) {
            GToast.error(this, "两次密码不一致"); return;
        }
        boolean ok = Communication.register(ip, user, p1, email);
        if (ok) {
            GToast.success(this, "注册成功，请使用新账号登录");
            if (onSuccess != null) onSuccess.accept(user);
            dispose();
        } else {
            GToast.error(this, "注册失败：用户名可能已存在或服务器不可达");
        }
    }
}
