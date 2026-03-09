{
  description = "CWM Devshell";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-25.11";
  };

  outputs = {nixpkgs, ...}: let
    system = "x86_64-linux";
    pkgs = import nixpkgs { inherit system; };
  in {
    devShells.${system}.default = pkgs.mkShell {
      buildInputs = with pkgs; [
          javaPackages.openjfx21
          libglibutil
          xorg.libXxf86vm
          glibc
          glib
          gsettings-desktop-schemas
          (pkgs.jdk21.override {enableJavaFX = true;})
          zlib
      ];
      packages = [
		pkgs.gradle
       ];
      shellHook = ''
        echo Welcome $(${pkgs.git}/bin/git config user.name)!
      '';

      #ENV
      LD_LIBRARY_PATH = "${pkgs.libGL}/lib:${pkgs.gtk3}/lib:${pkgs.glib.out}/lib:${pkgs.xorg.libXtst}/lib";
    };
  };
}