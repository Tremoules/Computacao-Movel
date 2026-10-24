# Assignment 4 - Remote Storage

Assignment 3's explorer, extended with **Firebase Realtime Database**, following
TP4's Firebase tutorial and the requirements on its final slide. This assignment
uses the slide's no-authentication/test-mode option and stores **image URLs**.

## Features

- **Save item** saves the displayed Character, Episode, or Location: its ID,
  category, name, image URL, and both displayed schema fields. Episodes and
  locations retain the related character image when one is available; items with
  no available image keep a null URL and the existing placeholder.
- **Load last** restores that snapshot and its category without requesting the
  item from the Rick and Morty API. **Browse API items** resumes normal browsing.
- **Total rolls** comes from a live Firebase listener. An accepted Roll click
  increments the database once; initial results, filters, loads, and clicks with
  no alternative item do not count as rolls.
- **Saved history** appends a new entry for every save, even repeated saves of
  the same item. The screen displays the newest 50; Firebase retains all entries.

## Firebase setup (the TP4 workflow)

Open **this `RemoteStorage` directory** in Android Studio, rather than the parent
`Computacao-Movel` repository.

1. In the [Firebase console](https://console.firebase.google.com/), create a
   project. Disable Google Analytics, as the slides instruct.
2. Add an **Android app** with package name `com.example.remotestorage`.
   This is RemoteStorage's unique `applicationId`, so Assignments 3 and 4 can be
   installed alongside each other. The source namespace is also
   `com.example.remotestorage`; Firebase registration uses the application ID.
3. Under **Build > Realtime Database**, create a database and choose **test
   mode**, as allowed on the assignment slide. Keep the generated test-mode
   expiry. No authentication providers or Cloud Storage bucket are required.
4. In **Project settings > General > Your apps**, download `google-services.json`
   **after creating the database**, and place it at
   `RemoteStorage/app/google-services.json`. This is the same location shown in
   the TP4 slides. The file is ignored by Git in this project.
5. Check that the JSON's `project_info.firebase_url` matches the Realtime Database
   URL shown in the console. For example, an EU database may use
   `https://YOUR-DATABASE.europe-west1.firebasedatabase.app`. If the field is
   missing, download an updated config; if necessary add `firebase_url` under
   `project_info` using the exact console URL.
6. Sync Gradle and rebuild/reinstall. The Google Services plugin reads the JSON
   and generates the Android resources used to initialize Firebase.

Without the configuration file or database URL, the API explorer still runs and
the storage panel explains the missing setup. It does not pretend to save data
locally. Adding the JSON requires a rebuild.

The Firebase dependencies use the BoM and the main `firebase-database` module.
The implementation uses `DatabaseReference`, `ValueEventListener`, `push()`, and
Firebase writes, as in the tutorial; the callbacks are adapted to coroutines and
Flow to fit the existing Compose/ViewModel app. Authentication from Friendly Chat
is omitted because the final assignment explicitly says it is unnecessary.

Test-mode rules permit unauthenticated access until their expiry. Use this only
for coursework/test data. If reads or writes are denied, check the database rules
and their expiry in the console. The app uses the `/remoteStorage` branch. Since
there is no login, all installations connected to this database share the latest
item, roll count, and history.

## Read the code in this order

1. `data/SavedExplorerItem.kt`: the database snapshot, separate from the API's
   network response models. Default constructor values let Firebase deserialize it.
2. `data/RemoteStorageRepository.kt`: the small persistence contract used by the
   ViewModel and replaced by a fake in unit tests.
3. `data/FirebaseRemoteStorageRepository.kt`: the Firebase implementation.
   `save()` uses one atomic `updateChildren()` for `lastItem` and the new history
   entry. `ServerValue.TIMESTAMP` records server time, and
   `ServerValue.increment(1)` prevents concurrent rolls from overwriting each other.
   `callbackFlow` converts live callbacks into Flow; `awaitClose` removes listeners.
4. `data/RemoteStorageProvider.kt`: creates the repository from the tutorial's
   Firebase configuration. `ui/RemoteStorageApp.kt` supplies it to the
   ViewModel through a factory.
5. `ui/screens/RemoteStorageViewModel.kt`: coordinates API requests and database
   actions. Save captures the displayed item/category before suspending. A roll
   write is independent of the cancellable API request, so navigating does not
   drop an accepted roll. Loading replaces the API selection with the saved
   snapshot and cancels any previous API request.
6. `ui/screens/RemoteStorageUiState.kt` and `RemoteStoragePanel.kt`: state and UI
   for actions, count, history, loading indicators, retry, and result messages.
   `HomeScreen.kt` keeps the existing API explorer and displays saved snapshots
   without presenting their single item as a fresh API count.

The `data/` paths above are relative to
`app/src/main/java/com/example/remotestorage/`.

### Database structure

```text
remoteStorage/
  rollCount: 12
  lastItem/
    id: 1
    category: "Character"
    name: "Rick Sanchez"
    firstField: "Alive"
    secondField: "Human"
    imageUrl: "https://..."
    savedAt: <server timestamp in milliseconds>
  history/
    <Firebase push key>/
      <same snapshot fields, plus relatedCharacterName when applicable>
```

Firebase omits null properties. A null `imageUrl` means there was no image
available for that item, not that loading it requires another API call.

### Failure and offline behavior

Database errors are displayed separately from API errors, so a storage problem
does not remove a usable API item. Listener failures offer **Retry database
sync**. Save/load buttons prevent overlapping operations; category/filter/roll
changes are blocked while loading a saved item.

Firebase can queue writes while offline. After 15 seconds the UI stops waiting
and explains that the operation is unconfirmed. Cancelling that wait does not
cancel a Firebase write, which may complete after reconnection. Check history or
the count before repeating it. There is no automatic write retry that could
create duplicate history entries or count a roll twice. Listener values can
include Firebase's local pending writes, so a displayed value is not itself a
server acknowledgement. Success messages are shown only after write completion.
Disk persistence is not enabled: pending writes are not guaranteed to survive
process termination.

## Verification

From this directory on Windows:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

The existing API tests remain, with additional ViewModel tests for all saved
categories, duplicate-click handling, repeated-save history, snapshot restoration,
roll counts and live changes, navigation during writes, missing/invalid saved
data, failures, listener retry, and timeout messages. These use a fake repository
and do not require Firebase credentials.

After Firebase setup, check the real database:

1. Save one item from each category and inspect `lastItem` and `history` in the
   console. Save the same item twice: two history entries should be present.
2. Change category/filter, then **Load last**. Confirm the saved name, category,
   fields, and image are restored. Restart the app and repeat.
3. Roll several times and restart. Confirm the counter is retained. A second
   device using the same database should see live updates.
4. Try loading from an empty database, and temporarily deny access to check the
   error messages and retry. Disconnect/reconnect the network to inspect pending
   writes and timeout behavior.

## References

- TP4 - Persistent Data, especially the Firebase configuration slides and final
  Assignment 4 slide.
- [Firebase Android codelab linked by TP4](https://firebase.google.com/codelabs/firebase-android)
- [Android Firebase setup](https://firebase.google.com/docs/android/setup)
- [Realtime Database reads, writes, atomic updates and increments](https://firebase.google.com/docs/database/android/read-and-write)
