package com.fuelstation.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.net.URI;

@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="app.browser.open", havingValue="true")
public class BrowserOpener {

    @EventListener({ApplicationReadyEvent.class})
    public void applicationReadyEvent() {
        String url = "http://localhost:8080";
        System.out.println("==========================================================");
        System.out.println(" Application ready! Access URL: " + url);
        System.out.println("==========================================================");
        openBrowser(url);
    }

    public static void openBrowser(String url) {
        String os = System.getProperty("os.name", "").toLowerCase();
        try {
            if (os.contains("mac") || os.contains("darwin")) {
                Runtime.getRuntime().exec(new String[]{"open", url});
                System.out.println("Browser opened via macOS 'open' command: " + url);
                return;
            }
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                System.out.println("Browser opened via Desktop API: " + url);
                return;
            }
            if (os.contains("win")) {
                Runtime.getRuntime().exec(new String[]{"cmd.exe", "/c", "start", url});
                System.out.println("Browser opened via Windows start command: " + url);
                return;
            }
            if (os.contains("nix") || os.contains("nux")) {
                Runtime.getRuntime().exec(new String[]{"xdg-open", url});
                System.out.println("Browser opened via Linux xdg-open: " + url);
                return;
            }
        } catch (Exception e) {
            System.err.println("Could not auto-open browser (" + e.getMessage() + "). Please open " + url + " in your browser.");
        }
    }
}
