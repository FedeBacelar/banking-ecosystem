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

The theme is responsible only for authentication screens rendered by Keycloak. Banking product screens belong to `banking-web` and are not implemented inside Keycloak.

Current covered screens:

```txt
login
error
info
login-page-expired
logout-confirm
```

Banking access requests, password recovery and long-lived browser sessions are not enabled from the public login screen. Those flows should be designed as controlled banking operations before exposing them to users.

To apply the theme in an existing local realm:

```txt
Realm settings -> Themes -> Login theme -> banking
```

If the realm is recreated from the import file, the login theme is configured automatically.
