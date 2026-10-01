# honya-dev-forks/fcitx5-android

Fcitx5 for Android のローカルフォーク。日本語（Anthy）で「ー」「？」を打ちやすくし、
ATOK for Android のQWERTY配置を参考にしたキーボードへ変更している。

## 変更内容

ブランチ `honya/add-japanese-symbol-keys`:

- 最上段に数字段を追加（ATOK/JIS配列と同じ。QWERTY段の副ラベル数字は削除）
- `-`（Anthyが「ー」に変換）をASDF段の右端へ配置
- 最下段を `記号/絵文字` / `@` / `🌐` / `space` / `。` / `◀` / `▶` / `⏎` に変更
  - 記号/絵文字キー: タップで数字パッド（`?123`）ページ、長押しで絵文字ピッカー
  - `@` は主面1タップ（メールアドレス用）。`,`（Anthyで`、`）は記号面の左下
  - `◀` `▶` は押しっぱなしで連続移動
- スワイプ用の副ラベル（英字・数字キーの下の小さい記号）を削除。記号は記号ページから入力する
- 英字ラベルは小文字表示
- 記号ページを2面化（`SymbolKeyboard`）
  - 数字パッドの `!?#` で記号面へ。`@ # $ % & - + ( ) / * " ' : ; ! ? = _ \ , . ~ ^ | [ ] { }` と数字段を配置
  - 記号面の `ABC` で英字面、`?123` で数字パッドへ戻る
  - 記号面の `!?#` は従来の網羅リスト（記号ピッカー）を開く

「？」「@」などは主面に置かず、記号ページ（`?123` → `!?#`）側にある（ATOKと同じ構成）。

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

## Mozcプラグイン（Android・試行中）

`honya/mozc-trial` ブランチ。OSS Mozc（`fcitx5-mozc`）をAndroid向けに組む試行。上流の `mozc` ブランチ/PR #941 は未マージで、prebuiltの`libmozc-static.a`も配布されていないため、mozc本体を自前でクロスビルドする。

### ビルド手順（.20 / arm64-v8a で確認、2026-09-23）

1. `git submodule update --init --recursive plugin/mozc/src/main/cpp/fcitx5-mozc`（`fcitx5-mozc` master → mozc / protobuf 34.1 / abseil を取得。約280MB）
2. host protoc を用意: `protobuf` submodule から `cmake --build /tmp/protoc-host --target protoc` し、`~/.local/bin/protoc` へ配置（クロスビルドでは host 側 protoc が必須）
3. `python` シム: `ln -sf "$(command -v python3)" ~/.local/bin/python`（mozc の生成スクリプトが `python` を呼ぶ）
4. Android向け差分はCMakeが自動適用する（`android_util.cc`の追加と、ビルドディレクトリへコピーした2ファイルへのパッチ）。submoduleへの手動パッチ適用は不要。
5. OSSデータ生成: `cd plugin/mozc/src/main/cpp/fcitx5-mozc/mozc/src && bazel build //data_manager/oss:mozc_dataset_for_oss@header` → `bazel-bin/data_manager/oss/mozc_data.inc`（48MB）を `data_manager/oss/` へコピー
6. プラグイン: `./gradlew :plugin:mozc:assembleDebug -PbuildABI=arm64-v8a`（`plugin/mozc/build.gradle.kts` に `buildFeatures { resValues = true }` が必須）

### 成果物と配備

- `plugin/mozc/build/outputs/apk/debug/org.fcitx.fcitx5.android.plugin.mozc-*-debug.apk`（arm64で21.8MB、`libmozc.so` 39MB＝OSS辞書・連接行列を内蔵）
- 配備は `adb install -r`（Anthyプラグインとは別APK。同時導入可）

### 辞書の更新

追加語彙は mozc の `data/dictionary_manual/{words.tsv,places.tsv}`（`よみ / 表記 / 品詞` のTSV）へ足し、手順5でデータを再生成する。語彙そのものは `honya-dev-forks/mozc-dict` で管理する。


## Mozcの変換・確定操作

画面キーボードでは以下の操作に対応する。物理キーボードのキー割り当ては従来どおり。

| 操作 | 動作 |
| --- | --- |
| 入力中の予測候補をタップ | 候補を確定する。部分候補なら残りの読みを保持する |
| 変換中の候補をタップ | 選んだ候補の文節まで確定し、残りを保持する |
| Space | 変換開始／次の候補 |
| 変換中の画面Enter | 先頭文節を確定し、残りを保持する |
| 未変換の画面Enter | 読みを確定する |
| Mozc入力中の画面Enter長押し | 入力全体を確定する。指を離しても改行を追加しない |
| 変換中の画面←／→ | 文節の区切りを縮小／拡大する |

Enter長押しの一括確定は、通常キーボード・候補展開・記号画面で利用できる。
未確定文字がないとき、または他のIMEでは従来の長押しメニューを維持する。
ATOK Androidの操作を参考にしているが、辞書や候補順位を含む完全互換ではない。

### ビルドと回帰検証

本体とMozcプラグインの両方を更新する。前述のSDK、host protoc、生成済みOSS辞書が必要。
`~/.local/bin/protoc --version`が実行できることを確認する。一時フォルダ内のprotocへのリンクが
切れていたら、固定されたprotobuf submoduleと同じ版を再ビルドしてから進める。

```bash
ECM_DIR=$HOME/ecm/usr/share/ECM/cmake ./gradlew :app:assembleDebug :plugin:mozc:assembleDebug -PbuildABI=arm64-v8a
# 既存のGradleタスク依存を切り分けるため、テストとlintは個別に実行する
./gradlew :app:testDebugUnitTest -PbuildABI=arm64-v8a
./gradlew :app:lintDebug -PbuildABI=arm64-v8a
```

Android統合テストは`MozcConversionTest`（実エンジン・辞書で候補確定、文節保持、画面キーと
物理キーの違い）と`MozcReturnGestureTest`（長押し後の二重確定・改行防止、既存メニュー保持）。
対象ABIで本体・Mozcプラグイン・テストAPKをビルドし、専用テスト端末へ導入して実行する。
`MozcConversionTest`はテスト端末の有効IMEをMozcにするため、日常使用端末ではなく専用環境を使う。

```bash
./gradlew :app:assembleDebug :plugin:mozc:assembleDebug :app:assembleDebugAndroidTest -PbuildABI=x86_64
# 対象を確認して3つのAPKを adb -s SERIAL install -r PATH で導入
adb -s SERIAL shell am instrument -w -e class org.fcitx.fcitx5.android.MozcConversionTest,org.fcitx.fcitx5.android.MozcReturnGestureTest org.fcitx.fcitx5.android.debug.test/androidx.test.runner.AndroidJUnitRunner
```
