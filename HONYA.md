# honya-dev-forks/fcitx5-android

Fcitx5 for Android のローカルフォーク。日本語（Anthy）で「ー」「？」を打ちやすくし、
ATOK for Android のQWERTY配置を参考にしたキーボードへ変更している。

## 変更内容

ブランチ `honya/add-japanese-symbol-keys`:

- 最上段に数字段を追加（ATOK/JIS配列と同じ。QWERTY段の副ラベル数字は削除）
- `-`（Anthyが「ー」に変換）をASDF段の右端へ配置
- 最下段を `記号/絵文字` / `、` / `🌐` / `space` / `。` / `◀` / `▶` / `⏎` に変更
  - 記号/絵文字キー: タップで数字パッド（記号）ページ、長押しで絵文字ピッカー
  - `◀` `▶` は押しっぱなしで連続移動
- スワイプ用の副ラベル（英字・数字キーの下の小さい記号）を削除。記号は記号ページから入力する
- 英字ラベルは小文字表示

「？」は主面に置かず、記号ページ（`?123` → `!?#`）側にある（ATOKと同じ構成）。

## ビルド（.20 / r5600x で確認）

必要なもの:

- Android SDK: `platforms;android-36`, `ndk;28.0.13004108`, `cmake;3.31.6`, `build-tools;36.1.0`
- JDK 21
- extra-cmake-modules（root不要でローカル展開可）

```bash
mkdir -p ~/ecm && cd ~/ecm && apt-get download extra-cmake-modules && dpkg-deb -x extra-cmake-modules_*.deb ~/ecm

cd ~/repos/fcitx5-android
git submodule update --init --recursive --depth 1 \
  lib/fcitx5/src/main/cpp/fcitx5 lib/fcitx5/src/main/cpp/prebuilt \
  lib/fcitx5-lua/src/main/cpp/fcitx5-lua lib/libime/src/main/cpp/libime \
  lib/fcitx5-chinese-addons/src/main/cpp/fcitx5-chinese-addons \
  plugin/anthy/src/main/cpp/anthy-cmake plugin/anthy/src/main/cpp/anthy-unicode \
  plugin/anthy/src/main/cpp/fcitx5-anthy

echo "sdk.dir=$HOME/Android/Sdk" > local.properties
ECM_DIR=$HOME/ecm/usr/share/ECM/cmake \
  ./gradlew :app:assembleDebug :plugin:anthy:assembleDebug -PbuildABI=arm64-v8a
```

出力:

- `app/build/outputs/apk/debug/org.fcitx.fcitx5.android-*-debug.apk`
- `plugin/anthy/build/outputs/apk/debug/org.fcitx.fcitx5.android.plugin.anthy-*-debug.apk`

## 端末への導入

デバッグビルドは applicationId が `org.fcitx.fcitx5.android.debug` 系になるため、
公式版とは別アプリとして共存できる（公式版のデータを消さずに試せる）。
署名が異なるため、公式版から乗り換える場合は公式版のアンインストールが必要。

```bash
adb -s <serial> install -r app/build/outputs/apk/debug/*.apk
adb -s <serial> install -r plugin/anthy/build/outputs/apk/debug/*.apk
adb -s <serial> shell ime enable org.fcitx.fcitx5.android.debug/org.fcitx.fcitx5.android.input.FcitxInputMethodService
adb -s <serial> shell ime set    org.fcitx.fcitx5.android.debug/org.fcitx.fcitx5.android.input.FcitxInputMethodService
```

アプリ内設定のうち、端末へ入れておくとよい値:

- 「スペースキー長押し時の動作」= 「インプットメゾッドのアクティブな状態を切り替える」（かな⇄英数）
- 「キーボードの高さ」= 既定（縦30% / 横49%）

デバッグビルドは `run-as` が使えるので、SharedPreferences を直接触って設定できる:

```bash
adb -s <serial> exec-out run-as org.fcitx.fcitx5.android.debug \
  cat shared_prefs/org.fcitx.fcitx5.android.debug_preferences.xml
```
