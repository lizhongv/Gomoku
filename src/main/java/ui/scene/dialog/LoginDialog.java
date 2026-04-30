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
import ui.scene.LobbyScene;
import ui.scene.Router;

public class LoginDialog extends GDialog {
    private final Router router;
    private final Consumer<Communication> onLoginSuccess;
    private final GTextField ipField;
    private final GTextField userField;
    private final JPasswordField pwdField;

    public LoginDialog(Window owner, Router router, Consumer<Communication> onLoginSuccess) {
        super(owner, "登录到服务器", true);
        this.router = router;
        this.onLoginSuccess = onLoginSuccess;
        Tokens t = Theme.tokens();

        ipField = new GTextField(16);
        ipField.setText("127.0.0.1");
        ipField.setPlaceholder("服务器 IP");

        userField = new GTextField(16);
        userField.setPlaceholder("用户名");

        pwdField = new JPasswordField(16);
        pwdField.setEchoChar('●');
        pwdField.setFont(t.fontBody);
        pwdField.setBorder(BorderFactory.createEmptyBorder(t.space2, t.space3, t.space2, t.space3));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(t.space2, t.space2, t.space2, t.space2);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0; c.gridy = 0; c.weightx = 0;
        form.add(label("服务器 IP："), c);
        c.gridx = 1; c.weightx = 1;
        form.add(ipField, c);

        c.gridx = 0; c.gridy = 1; c.weightx = 0;
        form.add(label("用户名："), c);
        c.gridx = 1; c.weightx = 1;
        form.add(userField, c);

        c.gridx = 0; c.gridy = 2; c.weightx = 0;
        form.add(label("密  码："), c);
        c.gridx = 1; c.weightx = 1;
        form.add(pwdField, c);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, t.space2, 0));
        buttons.setOpaque(false);
        GButton login = GButton.primary("登录");
        GButton register = GButton.secondary("注册");
        GButton cancel = GButton.ghost("取消");
        login.addActionListener(e -> doLogin());
        register.addActionListener(e -> {
            RegisterDialog rd = new RegisterDialog(owner, name -> userField.setText(name));
            rd.setVisible(true);
        });
        cancel.addActionListener(e -> dispose());
        buttons.add(login);
        buttons.add(register);
        buttons.add(cancel);

        content.add(form, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);

        setSize(new Dimension(400, 280));
        setLocationRelativeTo(owner);
    }

    private JLabel label(String text) {
        Tokens t = Theme.tokens();
        JLabel l = new JLabel(text);
        l.setFont(t.fontBody);
        l.setForeground(t.fgDefault);
        return l;
    }

    private void doLogin() {
        String ip = ipField.getText().trim();
        String user = userField.getText().trim();
        String pwd = new String(pwdField.getPassword());
        if (ip.isEmpty() || user.isEmpty()) {
            GToast.error(this, "请填写 IP 和用户名");
            return;
        }
        try {
            LobbyScene lobby = new LobbyScene(router, null);
            Communication comm = new Communication(lobby);
            String status = comm.loginStatus(ip, user, pwd);
            switch (status) {
                case "ok":
                    lobby.attachComm(comm);
                    dispose();
                    if (onLoginSuccess != null) onLoginSuccess.accept(comm);
                    router.push(lobby);
                    break;
                case "hasLogin":
                    GToast.error(this, "该账号已在其他客户端登录");
                    break;
                case "fail":
                    GToast.error(this, "用户名或密码错误（也可能尚未注册）");
                    break;
                case "error":
                default:
                    GToast.error(this, "无法连接服务器，请确认 Gomoku-Server 已启动且 IP 正确");
                    break;
            }
        } catch (Exception ex) {
            GToast.error(this, "登录异常：" + ex.getClass().getSimpleName() + " " + ex.getMessage());
        }
    }
}
