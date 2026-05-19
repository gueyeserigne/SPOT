#!/bin/bash

# ==========================================================
# Script : update_spot_repository.sh
# Objectif :
# Mettre à jour le dépôt Git SPOT après ajout
# de nouveaux modules, sans versionner les fichiers .jar
# volumineux refusés par GitHub.
# ==========================================================

set -e

# Se déplacer à la racine du projet SPOT
cd ~/RECHERCHE/MATSIM/SPOT || exit 1

echo "=== Vérification du dépôt ==="
git status

echo "=== Protection contre les fichiers .jar volumineux ==="

# Ajouter les fichiers .jar au .gitignore s'ils n'y sont pas déjà
grep -qxF "*.jar" .gitignore || echo "*.jar" >> .gitignore

# Retirer les .jar du suivi Git sans les supprimer du disque
git rm --cached -r --ignore-unmatch *.jar

echo "=== Ajout des modules SPOT ==="
git add SPOT-Editor/ SPOT-Optimization/ .gitignore

echo "=== Vérification des fichiers ajoutés ==="
git status

echo "=== Création du commit si nécessaire ==="
if git diff --cached --quiet; then
    echo "Aucune modification à committer."
else
    git commit -m "Add SPOT-Editor and SPOT-Optimization modules"
fi

echo "=== Synchronisation avec GitHub ==="
git pull origin main --rebase

echo "=== Envoi vers GitHub ==="
git push origin main

echo ""
echo "Mise à jour du dépôt SPOT terminée avec succès."
