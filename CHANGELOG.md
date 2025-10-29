## Changelog

### Key Changes

*   **Interstitial Ad Flow Optimization:** The logic for handling interstitial ads has been improved for better performance and reliability.
*   **Remote Config Update:** The structure for the `inter_backup_config` key has been updated.

### Testing Request

We kindly request your help in testing the new interstitial ad flow to ensure there are no issues with this update. Please report any unexpected behavior.

### Updated `inter_backup_config` Format

The `inter_backup_config` key in Firebase Remote Config now uses the following JSON structure:

```json
{
  "number": 1,
  "ids": [
    "ca-app-pub-3940256099942544/1033173712",
    "ca-app-pub-3940256099942544/1033173712"
  ]
}
```
