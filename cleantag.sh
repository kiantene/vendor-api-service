#!/bin/bash

# Fetch all tags to ensure the local repository is up-to-date
git fetch --tags

# Initialize an array to store tags to delete
tags_to_delete=()

# Loop through all tags
for tag in $(git tag); do
    # Check if the tag matches the x.x.x pattern (e.g., 1.0.0)
    if [[ $tag =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
        echo "Keeping tag: $tag"
    else
        echo "Marked for deletion: $tag"
        # Add the tag to the array of tags to delete
        tags_to_delete+=($tag)
    fi
done

# Check if there are any tags to delete
if [ ${#tags_to_delete[@]} -ne 0 ]; then
    echo "Deleting tags: ${tags_to_delete[@]}"

    # Delete tags locally
    git tag -d "${tags_to_delete[@]}"

    # Delete tags from the remote repository
    git push origin --delete "${tags_to_delete[@]}"
else
    echo "No tags to delete."
fi

echo "Cleanup complete. Only x.x.x pattern tags have been kept."
