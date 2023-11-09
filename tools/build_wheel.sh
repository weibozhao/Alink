set -ex

branch=$1

base_dir=$(cd "$(dirname "$0")"; pwd -P)
root_dir="$base_dir/.."
mkdir -p "$base_dir"/wheels

git checkout "$branch"
cd "$root_dir"
mvn clean package shade:shade -DskipTests

mkdir -p "$base_dir"/wheels
mkdir -p python/target/package/pyalink/lib/
rm -rf python/target/package/build
rm -rf python/target/package/dist
rm -rf python/target/package/pyalink.egg-info
cp core/target/alink_core_*.jar python/target/package/pyalink/lib/
cp python/target/alink_python_*.jar python/target/package/pyalink/lib/
cd python/target/package
python3 setup.py bdist_wheel
cp dist/*.whl  "$base_dir"/wheels/
