# Reproducible Builds

## Release builds:

Release builds are signed using Samourai's Android signing certificate and are available for download from our [web site](https://samouraiwallet.com/download) or from our [code repo](https://code.samourai.io/wallet/samourai-wallet/-/tree/master/apk).

Verify that the integrity of the downloaded binary file by following [these instructions](https://docs.samourai.io/en/verification).

## Building reproducible builds

### Version information

These instructions are valid for Samourai Wallet APKs v0.99.98i.

### Environment

To reproduce a build your development environment must be using

* Android SDK 33.0.2
* Open Java 17.0.6

### Building unsigned production APK

Production release target:

1. Build Samourai Wallet from command line:
production release: `./gradlew clean assembleRelease`

2. Obtain SHA-256 hash of the resulting APK file:
production release: `shasum -a 256 app/build/outputs/apk/production/release/app-production-release-unsigned.apk` 

3. This is the SHA-256 hash of the unsigned APK. Save it for future reference.

## Removing signatures from our APKs

### Download

Release builds are signed using Samourai's Android signing certificate and are available for download from our [web site](https://samouraiwallet.com/download) or from our [code repo](https://code.samourai.io/wallet/samourai-wallet/-/tree/master/apk).

Verify that the integrity of the downloaded binary file by following [these instructions](https://docs.samourai.io/en/verification).

### Remove signatures

Samourai Wallet APKs that you produce yourself using Android Studio or that you download from our FDroid repo or from Google may contain additional signatures on top of our own. In order to detect signature data which will affect the hash value of the file use the following command line instructions.

1. list files contained in your unsigned build: `apktool d -o output_folder_unsigned app/build/outputs/apk/production/release/app-production-release-unsigned.apk`

2. list files contained in your signed build: `apktool d -o output_folder app/build/outputs/apk/production/release/sw-signed.apk`

3. display the differences between the two file lists: `diff -qr output_folder output_folder_unsigned`

4. The resulting output will look something like this:

		Only in output_folder/META-INF: MANIFEST.MF  
		
		Only in output_folder/META-INF: CERT.RSA  
		
		Only in output_folder/META-INF: CERT.SF

5. Remove the listed files from the signed APK: 
`zip -d sw-signed.apk "META-INF/MANIFEST.MF" "META-INF/CERT.SF" "META-INF/CERT.RSA"`

6. Check that the `zip -d` command terminated without any errors.

### Test reproducibility

Building unsigned production APK

Upon success of **Remove signatures** step 6 above, calculate the SHA-256 hash of the signaturer-stripped APK: `shasum -a 256 sw-signed.apk`

The calculated hash should match the hash value saved in **Building unsigned production APK** step 3 above.

