# Keycloak themes

This folder contains local Keycloak themes mounted by Docker Compose.

Current theme:

```txt
banking
```

Current scope:

```txt
banking/login
```

The theme is responsible only for authentication screens rendered by Keycloak. Banking product screens belong to the future frontend and should not be implemented inside Keycloak.

To apply the theme in an existing local realm:

```txt
Realm settings -> Themes -> Login theme -> banking
```

If the realm is recreated from the import file, the login theme is configured automatically.
