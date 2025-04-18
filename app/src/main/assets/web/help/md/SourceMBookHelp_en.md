# Book management interface help

* Logo in the upper right corner of the source
  * A green dot indicates that the source of the book has been discovered, and discovery is enabled
  * Red dots indicate that the book source is found but not enabled
  * No sign indicates that the source of the book has not been found
* There is a group menu in the upper right corner, you can filter books by group
* Included in more menu at top right
  * New book source
  * Local import
  * Network import
  * QR code import
  * Share the selected source
* More actions for selecting a source are found in the menu at the bottom right, and are specific to the selected source
  * Enable the selection
  * Disable the selection
  * Add group
  * Remove group
  * Enable Discovery
  * Disable discovery
  * Top selection
  * Select the bottom
  * Export the selection
  * Verify the selection
* Check the book source can be checked in batches, due to network and other reasons, the result is for reference only
  * "Verification success" means that all the selected verification items pass
  * Can normally identify the failure caused by the search is empty, the discovery is empty, the search (discovery) directory is empty, the search (discovery) body is empty, the check timeout, and the js execution error, and the other reasons are considered as the website failure
  * Check search preferentially uses the check keywords filled in by the book source, and uses the keywords entered by the user when none exists
  * "Invalid" book sources will be automatically screened after verification