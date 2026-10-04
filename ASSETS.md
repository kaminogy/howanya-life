# キャラクター素材の差し替え方

素材・アイコン・猫のふるまいのロジックは `shared` モジュールにあり、ウォッチ版（`wear`）とスマホ版（`mobile`）の両方で使われます。

動作ごとに連番の PNG を置いてビルドし直すだけで、仮の猫（コードで描いたもの）が素材の画像に切り替わります。コードの変更はいりません。

## 置き場所と名前

```
shared/src/main/assets/cat/
  idle/     01.png 02.png ...   ぼーっとする
  walk/     01.png 02.png ...   歩く
  run/      01.png 02.png ...   走る
  sleep/    01.png 02.png ...   寝る
  stretch/  01.png 02.png ...   背伸び（ループしない。最後のコマで止まる）
  laugh/    01.png 02.png ...   笑う
  cry/      01.png 02.png ...   泣く
  eat/      01.png 02.png ...   ごはんを食べる
```

- コマは**ファイル名の順**に再生します。`01.png, 02.png, …` のように、桁数をそろえた連番にしてください。
- フォルダが無い動作は仮の猫で表示されるので、素材は一部ずつ追加しても大丈夫です。

## 描き方の決まり

| 項目 | 推奨 |
|---|---|
| 形式 | 背景透過の PNG |
| サイズ | 正方形、256×256px 程度（大きい画像は自動で縮小されます） |
| 向き | **右向き**で描く（左に歩くときはアプリが左右反転します） |
| 接地位置 | 足の裏が画像の**下から約 8%**（256px なら y≈236px）に来るようにする |
| コマ数 | 1 動作あたり 2〜8 コマ程度 |
| 全コマ共通 | 同じ動作の中では、キャンバスのサイズと猫の位置をそろえる |

## 左右反転の設定

標準では、左へ歩くときに画像を左右反転します。正面向きのキャラクターなど反転させたくない場合は、`shared/src/main/assets/cat/settings.properties` に次の1行を書いてください。

```
mirrorWhenFacingLeft=false
```

## いま入っている素材（ほわにゃ）

`tools/HowanyaSprites.java` がプログラムで描いた PNG です。全動作とも4コマで、正面向きなので反転はオフにしています。描き直したい場合は、スクリプトを編集して次のコマンドを実行してください（`build/howanya-sheet.png` に全コマの一覧も出力されます）。

```
"/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/java" -Djava.awt.headless=true tools/HowanyaSprites.java
```

ほわにゃは豊和銀行の公式キャラクターで、著作権は同行にあります。個人で楽しむ範囲にとどめ、アプリを公開・配布する場合は事前に豊和銀行の許可を得てください。

## 再生速度（fps）

動作ごとの 1 秒あたりのコマ数は `shared/src/main/java/com/kamino/howanyalife/model/CatAction.kt` で決まっています。

| 動作 | fps | ループ |
|---|---|---|
| idle | 4 | する |
| walk | 8 | する |
| run | 12 | する |
| sleep | 2 | する |
| stretch | 6 | しない |
| laugh | 8 | する |
| cry | 6 | する |
| eat | 6 | する |

コマ数に合わせて変えたい場合は、`fps = …` の値を書き換えてください。

## アプリのアイコン

アイコンは公式画像そのものを使っています。元画像は、豊和銀行の公式キャラクター名募集ページ（公募ガイド https://koubo.jp/contest/355524 ）に掲載されている `chara_01.png`（背景透過、1114×1719px）です。

- 前景：`shared/src/main/res/drawable-nodpi/ic_launcher_foreground.png`（432×432px）。元画像を 22% に縮小し、ランチャーの丸い表示範囲に頭が収まり、下に胴体の上部が少し見える位置に配置しています。
- 背景色：`shared/src/main/res/values/colors.xml` の `ic_launcher_background`（公式ページの黄色 `#FFF100`）。
