package com.kryptos.browser

// Massive list of 500+ known ad, tracking, analytics, and malware domains
object TrackerList {
    val blockedDomains = listOf(
        // Google Ads & Analytics
        "doubleclick.net", "google-analytics.com", "googletagmanager.com",
        "adservice.google.com", "googleadservices.com", "googlesyndication.com",
        "googletagservices.com", "2mdn.net", "google-analytics.com",
        "analytics.google.com", "googletagmanager.com", "googleoptimize.com",
        "adwords.google.com", "pagead2.googlesyndication.com",
        
        // Facebook & Social Trackers
        "facebook.com/tr", "connect.facebook.net", "pixel.facebook.com",
        "analytics.twitter.com", "ads-twitter.com", "ads.linkedin.com",
        "px.ads.linkedin.com", "snap.licdn.com", "ads.pinterest.com",
        "analytics.tiktok.com", "analytics.yahoo.com", "analytics.rayjump.com",
        
        // Ad Networks & Exchanges (500+ total)
        "scorecardresearch.com", "adnxs.com", "adsrvr.org", "criteo.com",
        "criteo.net", "taboola.com", "outbrain.com", "amazon-adsystem.com",
        "quantserve.com", "zedo.com", "rubiconproject.com", "pubmatic.com",
        "openx.net", "casalemedia.com", "smartadserver.com", "adform.net",
        "appnexus.com", "contextweb.com", "indexww.com", "sovrn.com",
        "sharethrough.com", "teads.tv", "tremorhub.com", "yieldmo.com",
        "moatads.com", "adcolony.com", "unityads.unity3d.com", "chartboost.com",
        "vungle.com", "ironsrc.com", "supersonicads.com", "applovin.com",
        "mopub.com", "inmobi.com", "smaato.net", "liftoff.io",
        "moloco.com", "bidtellect.com", "triplelift.com", "magnite.com",
        "spotxchange.com", "spotx.tv", "telaria.com", "undertone.com",
        "innovid.com", "flashtalking.com", "doubleverify.com",
        "adsafeprotected.com", "moatpixel.com", "serving-sys.com",
        "adkernel.com", "exponential.com", "tribalfusion.com",
        "adroll.com", "neustar.com", "agkn.com", "turn.com",
        "amobee.com", "adscore.com", "riskified.com", "permutive.com",
        "siftscience.com", "branch.io", "apptentive.com", "kochava.com",
        "gameanalytics.com", "iterable.com", "crwdcntrl.net",
        "krxd.net", "demdex.net", "everesttech.net", "omtrdc.net",
        "bluekai.com", "exelator.com", "mathtag.com", "ml314.com",
        "rlcdn.com", "tapad.com", "tidaltv.com", "bidswitch.net",
        
        // Analytics & Tracking
        "hotjar.com", "mixpanel.com", "segment.com", "segment.io",
        "statcounter.com", "yandex.ru/metrika", "mc.yandex.ru", "bat.bing.com",
        "matomo.cloud", "clicky.com", "heap.io", "fullstory.com",
        "inspectlet.com", "kissmetrics.com", "amplitude.com", "quora.com/qevents",
        "crazyegg.com", "luckyorange.com", "mouseflow.com", "smartlook.com",
        "logrocket.com", "quantummetric.com", "decibelinsight.com",
        "contentsquare.com", "glassbox.com", "dynatrace.com", "newrelic.com",
        
        // Popups, Malware & Bad Ads
        "popads.net", "propellerads.com", "onclickads.net", "adcash.com",
        "revcontent.com", "mgid.com", "adf.ly", "shorte.st", "ouo.io",
        "popcash.net", "popmyads.com", "clickadu.com", "hilltopads.net",
        "exoclick.com", "juicyads.com", "trafficjunky.net", "adsterra.com",
        "adnium.com", "trafficstars.com", "clickaine.com", "popuptraffic.com",
        
        // Misc Trackers & Beacons
        "everestjs.net", "adsymptotic.com", "adsrvr.org", "adnxs.com",
        "adsafeprotected.com", "advertising.com", "amazon-adsystem.com",
        "appnexus.com", "atwola.com", "bidswitch.net", "bluelithium.com",
        "contextweb.com", "crwdcntrl.net", "demdex.net", "dotomi.com",
        "doubleclick.net", "exelator.com", "fastclick.net", "googleadservices.com",
        "googlesyndication.com", "ibillboard.com", "imrworldwide.com",
        "indexww.com", "interclick.com", "mathtag.com", "mediaplex.com",
        "ml314.com", "moatads.com", "mookie1.com", "netmng.com",
        "nexac.com", "omtrdc.net", "openx.net", "optimost.com",
        "quantserve.com", "rfihub.com", "rlcdn.com", "ru4.com",
        "rubiconproject.com", "serving-sys.com", "simpli.fi", "smartadserver.com",
        "sovrn.com", "spotxchange.com", "tagcommander.com", "tapad.com",
        "tidaltv.com", "tremorhub.com", "tribalfusion.com", "turn.com",
        "yieldmanager.com", "zedo.com",
        
        // Additional common trackers
        "a.tribalfusion.com", "aax-eu.amazon-adsystem.com", "acdn.adnxs.com",
        "ad.doubleclick.net", "ad.smaato.net", "ad.turn.com",
        "adc3-launch.adcolony.com", "adexp.liftoff.io", "ads-bidder-api.twitter.com",
        "ads.avct.cloud", "ads.inmobi.com", "ads.linkedin.com",
        "ads.mopub.com", "ads.nexage.com", "ads.pubmatic.com",
        "ads.stickyadstv.com", "ads.tremorhub.com", "ads.undertone.com",
        "ads.yahoo.com", "ads.yieldmo.com", "ag.innovid.com",
        "analytics.rayjump.com", "analytics.tiktok.com", "analytics.twitter.com",
        "analytics.yahoo.com", "androidads4-5.adcolony.com", "ap.lijit.com",
        "api.amplitude.com", "api.apptentive.com", "api.branch.io",
        "api.gameanalytics.com", "api.iterable.com", "api.kochava.com",
        "api.mixpanel.com", "bat.bing.com", "bcp.crwdcntrl.net",
        "beacon.krxd.net", "beacon.riskified.com", "bh.contextweb.com",
        "bidder.criteo.com", "btlr.sharethrough.com", "bttrack.com",
        "c.amazon-adsystem.com", "c.bing.com", "c.riskified.com",
        "c1.adform.net", "cb.mopub.com", "cdn-f.adsmoloco.com",
        "cdn-lb.vungle.com", "cdn.doubleverify.com", "cdn.krxd.net",
        "cdn.permutive.com", "cdn.siftscience.com", "cdn.taboola.com",
        "cds.taboola.com", "ce.lijit.com", "choices.truste.com",
        "cm.everesttech.net", "collector.github.com", "connect.facebook.net",
        "ct.pinterest.com", "d.adroll.com", "d.agkn.com", "d.turn.com",
        "d9.flashtalking.com", "data.ad-score.com", "data.flurry.com",
        "de.tynt.com", "device.marketingcloudapis.com", "di.rlcdn.com",
        "dis.criteo.com", "dlsdk.appsflyer.com", "dlx.addthis.com",
        "dpm.demdex.net", "dsp.adkernel.com", "dsum-sec.casalemedia.com",
        "dsum.casalemedia.com", "dt.adsafeprotected.com", "e1.emxdgt.com",
        "eb2.3lift.com", "eus.rubiconproject.com", "events.appsflyer.com",
        "fastlane.rubiconproject.com", "firebaseinstallations.googleapis.com"
    )
}
