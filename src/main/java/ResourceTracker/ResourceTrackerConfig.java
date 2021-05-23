package ResourceTracker;

/**
 * The ResourceTrackerConfig class, it dictates variables of which Package Name to track
 * Leave the trackOnlyPackageName field empty, if all variables (also from libraries) should
 * be tracked
 */
public class ResourceTrackerConfig {

    public String trackOnlyPackageName;

    public ResourceTrackerConfig() {};

    public void setTrackOnlyPackageName(String trackOnlyPackageName) {
        this.trackOnlyPackageName = trackOnlyPackageName;
    }

}