[English](readme.md)

# svgclock-ad

SVGファイルを用いて時計を表示するデスクトップユーティリティープログラムを Android 用に作ってみました。

元の svgclock-rs は[こちら](https://github.com/zuntan/svgclock-rs)です。

# 特徴

- Inkscape を用いて作成したSVGファイルを利用して、現在の時刻を表示します。
	- svgclock-rs で使用しているSVGファイルと互換です。一部機能はオミットしています。
- 7種類のデザインの時計を表示することができます。（Ver 0.1.0）
- アプリケーション画面上、または**ウィジェットで「アナログ」な時計を秒針まで含めて描画**します。
	- 普通はできないそうです。もしくは正確にアニメーションできなかったり、描画更新が止まったりするアプリが世の中には沢山あるとのことです。 Android SDKの仕様として、ウィジェットは30分間隔程度でしか更新できないそうです。
	- 本アプリでは、**ユーザーがウィジェット更新用サービスを手動でオンにする**ことで、秒未満での描画処理を実装しています。

# 注意点

本プログラムのウィジェットは表示を1秒未満で更新し続けます。そのため、使用しない状態よりバッテリーの電力消費が増える傾向となり、より早くバッテリーが減る可能性があります。

# スクリーンショット

- アクティビティ
	- <img src="screenshot/Screenshot_Activity_1.png" width="30%" >
	- <img src="screenshot/Screenshot_Activity_2.png" width="30%" >
- ウィジェット登録
	- <img src="screenshot/Screenshot_Widget_List.png" width="30%" >
- ウィジェット
	- <img src="screenshot/Screenshot_Widget_1.png" width="30%" >
	- <img src="screenshot/Screenshot_Widget_3.png" width="30%" >
	- <img src="screenshot/Screenshot_Widget_6.png" width="30%" >
	- <img src="screenshot/Screenshot_Widget_7.png" width="30%" >

# 導入

[リリースページ](https://github.com/zuntan/svgclock-ad/releases)から最新のバイナリをダウンロードしてください。

# 使い方

本アプリを起動すると時計と設定の画面になります。

- 時計部分をタッチすると、時計が大きく表示されます。もう一度時計をタッチするともとに戻ります。
- 時計部分を長押しすると、「ウィジェットの時計」を追加することができます。
- **「ウィジェットの時計」を動かすには、「Clock Service for Widget」を「RUN」にしてください。** 止める時は「STOP」にします。
- 設定では、
	- 秒針のカスタマイズができます。
	- 時計のテーマを変更することができます。
	- ダウンロード等して保存したカスタムテーマを使用することができます。カスタムテーマの作り方は、 [こちら](https://github.com/zuntan/svgclock-rs)を参照してください。

# デザイン募集！

作者は絵の心得があまりありません。素敵なデザインの時計をご提供いただければ嬉しいです。
githubのissueにて受け付けますのでよろしくおねがいします。


---
