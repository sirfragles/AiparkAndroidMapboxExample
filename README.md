![AIPARK: Artificial Intelligence Based Parking](https://raw.githubusercontent.com/Aipark-Matthias/AiparkiOSSDK/master/logo.png)  
  
# Aipark Parking Mapbox Example
  
This project shows you how to integrate the [ Aipark](https://www.aipark.io/) parking informations in a mapbox android application. 
The app shows parking informations on a mapbox map. You can click on the parking areas to get more informations about it and see the predicted or actual occupancy over the day. If you click on the map for a longer time, an optimal route is shown leading to a parking spot nearby the desired location.

You can use this app as a starting point to develop your own parking app. If you have any questions or hints we are happy to [ hear](mailto:info@aipark.io)  from you!
  
## Build App
### 1. API keys

First you need a mapbox access token and an Aipark API key. You get both for free. All you need to do is register for the services:

- Mapbox access token
	-  sign up at https://www.mapbox.com/signup/
	-  you will find your personal access token at https://www.mapbox.com/account/

- Aipark API key
	- sign up at https://studio.aipark.io/sign-up/
	-  you will find your personal API key at https://studio.aipark.io/personal-account-management

2. After you get both keys you need to add them to your personal gradle.properties file on your system:

	- In  **Linux and Mac** machine it is located under ~/.gradle/ directory.  
    - In  **Windows** machine it can be find under C:\Users\your_user_name\.gradle\ directory.  
    
    If gradle.properties file is not present under ~/.gradle/ directory then create one.
	All you need to do is to add these two lines:
	
```bash  
MapboxApiKey="put your mapbox access token here"
AiparkApiKey="put your Aipark API key here"
```  

### 2. Clone and open project in Android Studio

You can download Android Studio at https://developer.android.com/studio/.
After you installed it, you simple need to open the project as an existing project.

## Customize App
The app is designed to make it as easy as possible for you to build your own project on it.
A good starting point is the class AiParkApp (app/src/main/java/io/aipark/android/example/mapbox/AiParkApp). 
To interact with the map and receive events like a selected parking area, the concept of an eventbus is used (https://github.com/greenrobot/EventBus). So if you would like to trigger an action e.g. if a parking area is selected, all you need to do is to write a listener for the ParkingAreaSelectedEvent:

```java  
@Subscribe(threadMode = ThreadMode.MAIN)  
public void onParkingAreaSelected(final ParkingAreaSelectedEvent event) {  
    Log.i("mapEvent", "parking area selected " + event.getParkingAreaResult());  
    new ParkingAreaPopup(new ParkingAreaResult(event.getParkingAreaResult().getData(), event.getParkingAreaResult().getOccupancy()));  
}
```    

The class AiParkApp contains callbacks for different events and open simple popups to show you how to access e.g. informations about a selected parking area. To send an event e.g. to center a position on the map you simply put the event on the bus:

```java  
EventBus.getDefault().postSticky(new CenterPositionEvent(new MapLatLng(event.getAddressResult().getCoordinate().lat, event.getAddressResult().getCoordinate().lng)));
``` 

The following events are implemented:
| send/receive | class | purpose |  
| ----- | ----- | --------- |  
| send | CenterCurrentPositionEvent | center the current position of the user on the map |  
| send | CenterPositionEvent | center given position on the map |  
| receive | MapClickedEvent | gives you the position if the map get clicked by the user |
| send | CenterPositionEvent | center given position on the map |
| send | MapThemeChangeEvent | change the map them e.g. to DARK or LIGHT mode |
| receive | OnFailureEvent | fired if an exception is raised e.g. because of missing internet connection or location |
| receive | OptimalTripEvent | gives an optimal parking search route |
| send/receive | ParkingAreaSelectedEvent | gives you the selected parking area or you could select a parking area with this event |
| receive | ReloadMapItemsEvent | reload all map item |
| send/receive | SearchEvent | gives you a search event e.g. if the map is long click or let you search for a given destination |

To get all parking informations you need you can use the following code from everywhere in the project to access the [ Aipark SDK](https://github.com/AIPARK-Open-Source/AiparkAndroidSDK) .

```java  
 AiParkApp.getAiparkSDK().getApi()
``` 