# cordova-utils-plugin

```java
cordova plugin add https://github.com/zhangjianying/cordova-utils-plugin.git
```



```javascript
cordova.plugins.utils.isEmulator(function(ret){
			alert('isEmulator:'+ret);
			cordova.plugins.utils.isDeviceRooted(function(aret){
				alert('isDeviceRooted:'+aret);
				
			cordova.plugins.utils.isXposed(function(xret){
				alert('xposed:'+xret);
				cordova.plugins.utils.exitApp();
			},function(){
				
				
			});
				
			},function(){
					
			});
		
		},function(){
			
		});
  ```
  
  
 排除 省电名单  
```javascript

cordova.plugins.utils.batteryOptimization(function(){},
		function(){});

```