#!/bin/sh

export PATH=$PATH:"/c/bin/Python27"

echo -e "\nPerforce info: "
p4 -V

echo -e "\nPython info: "
python --version

echo -e "\nGit info: "
git --version

echo -e "\nGit global config: "
git config --global --list

echo -e "\n"

declare repo_name="sktools"
declare -a aktive_branches=("1.5" "5.x" "trunk") # only mapping out active branches as branches - the rest is tags only
declare git_p4=$PWD/bin/git-p4
declare repo_dir=$PWD/$repo_name

git config --unset-all git-p4.branchList
#using --detect-branches wich requires a single branch spec containing all mappings
# Example:
#//sktools/SKToolsKode/trunk/... //sktools/SKToolsKode/1.2/...
#//sktools/SKToolsKode/trunk/... //sktools/SKToolsKode/1.3/...
#//sktools/SKToolsKode/trunk/... //sktools/SKToolsKode/1.4/...
#//sktools/SKToolsKode/trunk/... //sktools/SKToolsKode/1.5/...
#//sktools/SKToolsKode/trunk/... //sktools/SKToolsKode/5.x/...


mkdir $repo_dir
cd $repo_dir
echo -e "Cloning into $repo_dir ..."
$git_p4 clone //sktools/SKToolsKode@all --detect-branches --import-labels --destination .


echo -e "\nAll refs: \n"
git show-ref


for BRANCH in ${aktive_branches[@]}; do
  echo "Defining branch $BRANCH ..."
  git checkout -b $BRANCH
	git reset refs/remotes/p4/SKToolsKode/$BRANCH --hard
done


echo -e "\n\nConverted tags: "
git tag --list

echo -e "\n\nConverted branches: "
git branch --list

echo -e " \n\nTo push branches to origin do:\n"
echo "git remote add origin <url>"
for BRANCH in ${aktive_branches[@]}; do
  echo "git checkout $BRANCH"
  echo "git push --set-upstream origin $BRANCH"
  echo "git push --tags"
done

echo -e "\nnJoy!"
