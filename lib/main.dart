import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'dart:io' show Platform;

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'WeFitter Flutter',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.blue),
        useMaterial3: true,
      ),
      home: const MyHomePage(title: 'WeFitter bridge to Health Connect'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key, required this.title});

  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  static const platform = MethodChannel('wefitter');
  static const platform2 = MethodChannel('wefitter_sdk');

  bool supported = true;
  bool configured = false;
  bool connected = false;
  String error = '';

  static const prefix = 'android.permission.health';
  static const myAppPermissions = {
    "$prefix.READ_DISTANCE",
    "$prefix.READ_STEPS",
    "$prefix.READ_TOTAL_CALORIES_BURNED",
    "$prefix.READ_HEART_RATE",
    "$prefix.READ_POWER",
    "$prefix.READ_EXERCISE",
    //"$prefix.READ_BLOOD_GLUCOSE",
    //"$prefix.READ_BLOOD_PRESSURE",
    //"$prefix.READ_BODY_FAT",
    //"$prefix.READ_BODY_TEMPERATURE",
    "$prefix.READ_HEIGHT",
    //"$prefix.READ_OXYGEN_SATURATION",'
    "$prefix.READ_WEIGHT",
    "$prefix.READ_SPEED",
    "$prefix.READ_SLEEP",
  };
  static var myAppPermissionsString = myAppPermissions.join(',');

  // create config
  static var config = {
    'token': 'YOUR_TOKEN', // required, WeFitter API profile bearer token
    'apiUrl': 'https://api.wefitter.com/api/', // optional, only use if you want to use your backend as a proxy and forward all API calls to the WeFitter API. Default: `https://api.wefitter.com/api/`
    // 'startDate': 'CUSTOM_START_DATE', // optional with format `yyyy-MM-dd`, by default data of the past 20 days will be uploaded
    // 'notificationTitle': 'CUSTOM_TITLE', // optional
    // 'notificationText': 'CUSTOM_TEXT', // optional
    // 'notificationIcon': 'CUSTOM_ICON', // optional, e.g. `ic_notification` placed in either drawable, mipmap or raw
    // 'notificationChannelId': 'CUSTOM_CHANNEL_ID', // optional
    // 'notificationChannelName': 'CUSTOM_CHANNEL_NAME', // optional
    'appPermissions': myAppPermissionsString,
  };

  @override
  void initState() {
    super.initState();
    platform2.setMethodCallHandler(nativeMethodCallHandler);
    platform.invokeMethod(
        "configure", {'config': config});
  }

  Future<dynamic> nativeMethodCallHandler(MethodCall methodCall) async {
    switch (methodCall.method) {
      case "onConfiguredWeFitterHealthConnect" :
        setState(() {
          configured = methodCall.arguments["configured"];
        });
        break;
      case "onConnectedWeFitterHealthConnect" :
        setState(() {
          connected = methodCall.arguments["connected"];
        });
        break;
      case "onSupported" :
        setState(() {
          supported = methodCall.arguments["supported"];
        });
        break;
      case "onErrorWeFitterHealthConnect" :
        setState(() {
          error = methodCall.arguments["error"];
        });
        break;
      default:
        return "Nothing";
        break;
    }
  }

  void _onPressConnectOrDisconnect() {
    setState(() {
      if (Platform.isAndroid) {
        platform.invokeMethod("isSupported");
        if (supported) {
          if (connected) {
            platform.invokeMethod("disconnect");
          } else {
            platform.invokeMethod("connect");
          }
        }
      } else {
        // Alert
      };
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        title: Text(widget.title),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            Text(
              'Configured: $configured',
              style: Theme.of(context).textTheme.bodyMedium,
            ),Text(
              'Connected: $connected',
              style: Theme.of(context).textTheme.bodyMedium,
            ),Text(
              error != '' ? 'Error: $error' : '',
              style: TextStyle(color: Colors.red.withOpacity(0.9)),
            ),
            TextButton(
              style: ButtonStyle(
                foregroundColor: WidgetStateProperty.all<Color>(Colors.white),
                backgroundColor: WidgetStateProperty.all<Color>(Colors.blue),
              ),
              onPressed: _onPressConnectOrDisconnect,
              child: Text(connected ? 'DISCONNECT' : 'CONNECT'),
            ),
          ],
        ),
      ),
    );
  }
}
