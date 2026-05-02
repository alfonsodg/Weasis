#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import java.awt.Component;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.JButton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.core.api.gui.util.GuiUtils;
import org.weasis.core.ui.util.WtoolBar;

public class SampleToolBar extends WtoolBar {

    private static final Logger LOGGER = LoggerFactory.getLogger(SampleToolBar.class);

    protected SampleToolBar() {
        super("Sample Toolbar", 400);

        final JButton helpButton = new JButton();
        helpButton.setToolTipText("User Guide");
        helpButton.putClientProperty("JButton.buttonType", "help");
        helpButton.addActionListener(
            e -> {
                if (e.getSource() instanceof Component component) {
                    try {
                        URL url = new URL("https://nroduit.github.io/en/tutorials/");
                        GuiUtils.openInDefaultBrowser(component, url);
                    } catch (MalformedURLException e1) {
                        LOGGER.error("Cannot open URL", e1);
                    }
                }
            });
        add(helpButton);
    }
}
