/* 
 * Copyright 2013 The Polymer Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */

(function(){var e="html-imports.js",t="HTMLImports",n=["src/HTMLImports.js"];window[t]={entryPointName:e,modules:n};var r=document.querySelector('script[src*="'+e+'"]'),i=r.attributes.src.value,s=i.slice(0,i.indexOf(e));if(!window.Loader){var o=s+"tools/loader/loader.js";document.write('<script src="'+o+'"></script>')}document.write('<script>Loader.load("'+t+'")</script>')})();