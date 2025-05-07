This is a new [**React Native**](https://reactnative.dev) project, bootstrapped using [`@react-native-community/cli`](https://github.com/react-native-community/cli).

# Getting Started

> **Note**: Make sure you have completed the [Set Up Your Environment](https://reactnative.dev/docs/set-up-your-environment) guide before proceeding.

## Step 1: Start Metro

First, you will need to run **Metro**, the JavaScript build tool for React Native.

To start the Metro dev server, run the following command from the root of your React Native project:

```sh
# Using npm
npm start

# OR using Yarn
yarn start
```

## Step 2: Build and run your app

With Metro running, open a new terminal window/pane from the root of your React Native project, and use one of the following commands to build and run your Android, iOS, or web app:

### Android

```sh
# Using npm
npm run android

# OR using Yarn
yarn android
```

### iOS

For iOS, remember to install CocoaPods dependencies (this only needs to be run on first clone or after updating native deps).

The first time you create a new project, run the Ruby bundler to install CocoaPods itself:

```sh
bundle install
```

Then, and every time you update your native dependencies, run:

```sh
bundle exec pod install
```

For more information, please visit [CocoaPods Getting Started guide](https://guides.cocoapods.org/using/getting-started.html).

```sh
# Using npm
npm run ios

# OR using Yarn
yarn ios
```

### Web

To run the app in a web browser:

```sh
# Using npm
npm run web

# OR using Yarn
yarn web
```

To build the web version for production:

```sh
# Using npm
npm run web-build

# OR using Yarn
yarn web-build
```

This will create a `dist` folder with the compiled web assets that can be deployed to any static hosting service.

If everything is set up correctly, you should see your new app running in the Android Emulator, iOS Simulator, web browser, or your connected device.

This is one way to run your app — you can also build it directly from Android Studio or Xcode.

## Step 3: Modify your app

Now that you have successfully run the app, let's make changes!

Open `App.tsx` in your text editor of choice and make some changes. When you save, your app will automatically update and reflect these changes — this is powered by [Fast Refresh](https://reactnative.dev/docs/fast-refresh).

When you want to forcefully reload, for example to reset the state of your app, you can perform a full reload:

- **Android**: Press the <kbd>R</kbd> key twice or select **"Reload"** from the **Dev Menu**, accessed via <kbd>Ctrl</kbd> + <kbd>M</kbd> (Windows/Linux) or <kbd>Cmd ⌘</kbd> + <kbd>M</kbd> (macOS).
- **iOS**: Press <kbd>R</kbd> in iOS Simulator.

## Congratulations! :tada:

You've successfully run and modified your React Native App. :partying_face:

### Now what?

- If you want to add this new React Native code to an existing application, check out the [Integration guide](https://reactnative.dev/docs/integration-with-existing-apps).
- If you're curious to learn more about React Native, check out the [docs](https://reactnative.dev/docs/getting-started).

# Troubleshooting

## Common Issues and Solutions

### Issue 1: Terminal App Specification Error

If you encounter this error:
```
error Cannot start server in new windows because no terminal app was specified, use --terminal to specify, or start a dev server manually by running npm start or yarn start in other terminal window.
```

You have several options:

1. **Use the dev-ios script** (recommended):
   ```sh
   npm run dev-ios
   # OR
   yarn dev-ios
   ```
   This script starts the Metro bundler first and then runs the iOS app without trying to start a new terminal window.

2. **Specify Terminal app explicitly**:
   ```sh
   npm run ios-terminal
   # OR
   yarn ios-terminal
   ```
   This uses the macOS Terminal app explicitly.

3. **Start Metro separately**:
   In one terminal window:
   ```sh
   npm start
   # OR
   yarn start
   ```

   Then in another terminal window:
   ```sh
   npm run ios
   # OR
   yarn ios
   ```

### Issue 2: Xcode Developer Directory Error

If you encounter this error:
```
xcode-select: error: tool 'xcodebuild' requires Xcode, but active developer directory '/Library/Developer/CommandLineTools' is a command line tools instance.
```

Run the setup-xcode script to set the correct Xcode path:
```sh
npm run setup-xcode
# OR
yarn setup-xcode
```

This will set the active developer directory to the full Xcode installation path.

Alternatively, you can run this command directly:
```sh
sudo xcode-select -s /Applications/Xcode.app/Contents/Developer
```

For other issues, see the [React Native Troubleshooting](https://reactnative.dev/docs/troubleshooting) page.

# Learn More

To learn more about React Native, take a look at the following resources:

- [React Native Website](https://reactnative.dev) - learn more about React Native.
- [Getting Started](https://reactnative.dev/docs/environment-setup) - an **overview** of React Native and how setup your environment.
- [Learn the Basics](https://reactnative.dev/docs/getting-started) - a **guided tour** of the React Native **basics**.
- [Blog](https://reactnative.dev/blog) - read the latest official React Native **Blog** posts.
- [`@facebook/react-native`](https://github.com/facebook/react-native) - the Open Source; GitHub **repository** for React Native.
