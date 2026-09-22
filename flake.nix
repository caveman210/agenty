{
  description = "Development environment and build flake for triage-agent";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs {
          inherit system;
          config.allowUnfree = true;
        };

        jdk = pkgs.openjdk21;
        maven = pkgs.maven;
      in
      {
        devShells.default = pkgs.mkShell {
          name = "triage-agent-shell";

          buildInputs = with pkgs; [
            openjdk21
            maven
            podman
            podman-compose
            minikube
            kubectl
            kubernetes-helm
            k9s
            curl
            jq
            httpie
          ];

          shellHook = ''
            export JAVA_HOME="${jdk}"
            export PATH="$JAVA_HOME/bin:$PATH"

            echo "Java:    $(java --version | head -n 1)"
            echo "Maven:   $(mvn --version | head -n 1)"
            echo "Kubectl: $(kubectl version --client -o yaml 2>/dev/null | grep gitVersion | head -n 1 || echo 'Installed')"
          '';
        };

        packages.default = pkgs.stdenv.mkDerivation {
          pname = "triage-agent";
          version = "1.0.0";
          src = ./.;

          nativeBuildInputs = [ maven jdk ];

          buildPhase = ''
            export HOME=$TMPDIR
            mvn clean package -DskipTests --offline -Dmaven.repo.local=$TMPDIR/.m2
          '';

          installPhase = ''
            mkdir -p $out/bin $out/share/java
            cp target/*.jar $out/share/java/app.jar
            makeWrapper ${jdk}/bin/java $out/bin/triage-agent \
              --add-flags "-jar $out/share/java/app.jar"
          '';
        };
      }
    );
}
