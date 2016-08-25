### NBWicketSourcePlugin

NBWicketSourcePlugin is a NetBeans plugin that works with the
[Wicket-Source](https://github.com/jennybrown8/wicket-source) browser extension.

It speeds up Wicket development by providing click-through from browser HTML back to the original Wicket components in your source

### Plugin Installation

At the moment you have to build the plugin from the sources. I uploaded the plugin to the NetBeans plugin portal today (25.08.2016), so it should be available within the next few days. 
In the meanwhile you can find a pre-compiled version in the releases directory.

### Add Wicket-Source to your project

1. Install the wicket-source jar into your project using maven or your preferred build tool.  Add it to your WicketApplication `init()` as
     `WicketSource.configure(this);`

2. Visit the Chrome Web Store and search for wicket to find the chrome plugin.

3. Install the NetBeans plugin. The plugin adds a button to the toolbar where you can start and stop listening to requests coming from the Chrome plugin.

See the [original Wiki page](https://github.com/jennybrown8/wicket-source/wiki) for deeper details.

### Further notes

At the moment no configuration is possible. That means: no password protection, no possibility for changing the default port to anything other than 9123.
I don't know if this is coming any time soon since I don't need it myself. 

### Acknowledgement

This code used George Armholds [wicket-source-intellij](https://github.com/armhold/wicket-source-intellij) project for inspiration
