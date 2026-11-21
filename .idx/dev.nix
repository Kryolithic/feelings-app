{ pkgs, ... }: {
  channel = "stable-24.11";
  packages = [
    pkgs.nodejs_22
    pkgs.firebase-tools
  ];

  idx = {
    extensions = [];
    workspace = {
      onCreate = {
        npm-install = "npm --prefix web install";
        default.openFiles = [
          "web/src/App.tsx"
          "web/.env.example"
        ];
      };
    };
    previews = {
      enable = true;
      previews = {
        web = {
          command = [
            "npm"
            "run"
            "dev"
            "--"
            "--port"
            "$PORT"
            "--host"
            "0.0.0.0"
          ];
          manager = "web";
          cwd = "web";
        };
      };
    };
  };
}
