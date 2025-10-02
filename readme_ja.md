[English](readme.md)

# svgclock-ad

SVGファイルを用いて時計を表示するデスクトップユーティリティープログラムを Android 用に作ってみました。

元の svgclock-rs は[こちら](https://github.com/zuntan/svgclock-rs)です。

# 特徴

- Inkscape を用いて作成したSVGファイルを利用して、現在の時刻を表示します。
	- svgclock-rs で使用しているSVGファイルと互換です。一部機能はオミットしています。
- 7種類のデザインの時計を表示することができます。（Ver 0.1.0）
- アプリケーション画面上、または**ウィジェットで「アナログ」な時計を秒針まで含めて描画**します。
	- 普通はできないそうです。もしくは正確にアニメーションできなかったり、描画更新が止まったりするアプリが世の中には沢山あるとのことです。
		- Android SDKの仕様として、ウィジェットは30分間隔程度でしか更新できないそうです。
	- 本アプリも、**ユーザーがウィジェット更新用サービスをオンにする**ことで、秒未満での描画処理を実装しています。
		- 今後も研究します。

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


---
