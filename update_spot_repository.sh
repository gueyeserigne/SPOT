#!/bin/bash

# ==========================================================
# Script : update_spot_repository.sh
# Objectif :
# Mettre à jour le dépôt Git SPOT après ajout
# de nouveaux modules (SPOT-Editor, SPOT-Optimization)
# ==========================================================

# Se déplacer à la racine du projet SPOT
# Permet d'exécuter toutes les commandes Git
# dans le bon dépôt.
cd ~/RECHERCHE/MATSIM/SPOT || exit

# Afficher l'état actuel du dépôt
# Montre :
# - fichiers modifiés
# - nouveaux fichiers
# - fichiers supprimés
# - fichiers non suivis
echo "=== Etat actuel du dépôt ==="
git status

# Ajouter tous les nouveaux fichiers et modifications
#
# Equivalent à :
# git add SPOT-Editor SPOT-Optimization
#
# Le point "." signifie :
# ajouter tout ce qui a changé dans le projet
echo "=== Ajout des nouveaux fichiers ==="
git add .

# Créer un commit local
#
# Le commit représente un instantané des modifications.
#
# Modifier le message selon les changements réalisés.
echo "=== Création du commit ==="
git commit -m "Add SPOT-Editor and SPOT-Optimization modules"

# Synchroniser d'abord avec le dépôt distant
#
# --rebase évite les historiques parallèles
# et place les commits locaux après les commits distants
#
# Cela résout le problème :
# "fetch first"
echo "=== Synchronisation avec GitHub ==="
git pull origin main --rebase

# Vérifier si l'étape précédente a réussi
if [ $? -ne 0 ]; then
    echo ""
    echo "Conflit détecté pendant le rebase."
    echo "Résoudre les conflits puis exécuter :"
    echo "git rebase --continue"
    exit 1
fi

# Envoyer les modifications vers GitHub
#
# Le dépôt distant est généralement appelé origin
# main est la branche principale
echo "=== Envoi vers GitHub ==="
git push origin main

# Afficher un message de confirmation
echo ""
echo "Mise à jour du dépôt SPOT terminée avec succès."
