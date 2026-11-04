# Privacy notes for this fork

Continue Watching reads only TV-provider entries exposed to the application with normal READ_TV_LISTINGS permission. It does not query private app databases, scrape private interfaces or request ACCESS_ALL_EPG_DATA.

Published titles, artwork links, original resume intents and progress are cached in private application storage. Artwork uses a bounded local cache. These caches can reveal viewing history and must not be shared. Revoking TV-listings permission hides content and clears the feature's metadata cache when refreshed.

Catalog and artwork requests contact their providers. No new telemetry or watch-history upload was added by this fork. Optional upstream network sources remain subject to their own behavior and provider policies.

Fast Home matches stock launcher window package/class events. It cannot read window contents, filter remote keys or perform gestures. The fallback watcher uses Android usage access and overlay permission.

Keep local configuration, credentials, build/signing keys, device identifiers, screenshots with personal information and TV diagnostic files outside Git. The repository ignores common local/private paths; review every staged file before publishing.
