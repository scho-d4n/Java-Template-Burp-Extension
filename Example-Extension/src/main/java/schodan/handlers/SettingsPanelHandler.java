package schodan.handlers;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.settings.SettingsPanelBuilder;
import burp.api.montoya.ui.settings.SettingsPanelPersistence;
import burp.api.montoya.ui.settings.SettingsPanelSetting;
import burp.api.montoya.ui.settings.SettingsPanelWithData;

import java.util.List;

/**
 * Skeleton handler for a Burp settings panel with persistence.
 * Builds a panel via SettingsPanelBuilder (USER_SETTINGS, PROJECT_SETTINGS, or NONE),
 * exposes the panel for registration and getters for use elsewhere.
 *
 * @see <a href="https://portswigger.net/burp/documentation/desktop/extend-burp/extensions/creating/tutorials/settings-panel">Adding a settings panel</a>
 */
public class SettingsPanelHandler {

    private static final String SETTING_TARGET_HOST = "Target host";
    private static final String SETTING_MAX_REQUESTS = "Max requests";
    private static final String SETTING_FOLLOW_REDIRECTS = "Follow redirects";
    private static final String SETTING_MODE = "Mode";

    private final MontoyaApi montoyaApi;
    private final SettingsPanelWithData panel;

    public SettingsPanelHandler(MontoyaApi montoyaApi) {
        this.montoyaApi = montoyaApi;
        this.panel = buildPanel();
    }

    /**
     * Build the panel with persistence and example settings.
     * Persistence: USER_SETTINGS (across projects), PROJECT_SETTINGS (per project), or NONE.
     */
    private SettingsPanelWithData buildPanel() {
        return SettingsPanelBuilder.settingsPanel()
                .withPersistence(SettingsPanelPersistence.USER_SETTINGS)
                .withTitle("Template extension settings")
                .withDescription("Example settings panel with string, integer, boolean, and list. " +
                        "USER_SETTINGS: saved in user data across projects. " +
                        "Use PROJECT_SETTINGS for per-project, or NONE for no persistence.")
                .withKeywords("Template", "Settings", "Example")
                .withSettings(
                        SettingsPanelSetting.stringSetting(SETTING_TARGET_HOST, "example.com"),
                        SettingsPanelSetting.integerSetting(SETTING_MAX_REQUESTS, 100),
                        SettingsPanelSetting.booleanSetting(SETTING_FOLLOW_REDIRECTS, true),
                        SettingsPanelSetting.listSetting(SETTING_MODE,
                                List.of("Sniper", "Battering ram", "Pitchfork", "Cluster bomb"),
                                "Sniper")
                )
                .build();
    }

    /** Return the panel for registration with {@code api.userInterface().registerSettingsPanel(panel)}. */
    public SettingsPanelWithData getPanel() {
        return panel;
    }

    // --- Getters: read current values from the panel (use in your extension logic) ---

    public String getTargetHost() {
        return panel.getString(SETTING_TARGET_HOST);
    }

    public int getMaxRequests() {
        return panel.getInteger(SETTING_MAX_REQUESTS);
    }

    public boolean getFollowRedirects() {
        return panel.getBoolean(SETTING_FOLLOW_REDIRECTS);
    }

    /** List setting value is retrieved as string. */
    public String getMode() {
        return panel.getString(SETTING_MODE);
    }

    /** Example: log current settings (e.g. when a feature runs). */
    public void logCurrentSettings() {
        montoyaApi.logging().logToOutput("[Settings] Target host: " + getTargetHost());
        montoyaApi.logging().logToOutput("[Settings] Max requests: " + getMaxRequests());
        montoyaApi.logging().logToOutput("[Settings] Follow redirects: " + getFollowRedirects());
        montoyaApi.logging().logToOutput("[Settings] Mode: " + getMode());
    }
}
