# Synopsis

Ring middleware for transforming XML files. This library's reason for
existence is to facilitate static includes and wrappers for HTML files, but
there is no reason why it should be limited to that application.

There is a bundled stylesheet which uses tags in the
`http://github.com/jedahu/ring-xslt/` namespace: `wrap`, `provide`, `use`, and
`include`.

## Example

With the files `main.html`,

~~~~
<html xmlns='http://www.w3.org/1999/xhtml'
      xmlns:rx='http://github.com/jedahu/ring-xslt/'>
  <head>
    <title><rx:use name='title'/></title>
  </head>
  <body>
    <article>
      <rx:use/>
    </article>
    <rx:include href='footer.html'/>
  </body>
</html>
~~~~

`jabberwock.html`,

~~~~
<rx:wrap href='main.html' xmlns:rx='http://github.com/jedahu/ring-xslt/'>
  <rx:provide name='title'>Jabberwock</rx:provide>
  <p>'Twas brillig and the slithy toves...</p>
</rx:wrap>
~~~~

and `footer.html`,

~~~~
<p>Site designed by the Queen of Hearts.</p>
~~~~

and this use of Ring XSLT:

<pre class='brush: clojure'>
(require '[ring.middleware.xslt :as rx])
(use 'ring.adapter.jetty)

;; process-xslt [handler stylesheet file-root & opts]

(run-jetty
  (rx/process-xslt
    (constantly {:status 404})
    (rx/static-wrap)
    "path/to/html/files/"
    :from :file)
  {:port 8080})
</pre>

the content of `http://localhost:8080/jabberwock.html` will be:

~~~~
<html xmlns='http://www.w3.org/1999/xhtml'>
  <head>
    <title>Jabberwock</title>
  </head>
  <body>
    <article>
      <p>'Twas brillig and the slithy toves...</p>
    </article>
    <p>Site designed by the Queen of Hearts</p>
  </body>
</html>
~~~~

## Tags

### rx:include

Like XInclude, but the stylesheet controls when it is processed.

~~~~
<rx:include href='path/to/xml/file#fragment-id'/>
~~~~

href
: (required) URI of a resource to include.

### rx:wrap

Wrap fragment(s) of XML inside another XML resource.

~~~~
<rx:wrap href='path/to/xml/file'>
  <!-- content -->
</rx:wrap>
~~~~

href
:  (required) URI of a resource containing `rx:use` elements.

content
:  (required) A sequence of `rx:provide` elements corresponding to the
   `rx:use` elements in the resource. If the resource contains a `rx:use`
   element without a `name` attribute, the content may contain one
   `rx:provide` element without a `name` attribute, otherwise all elements
   after the sequence of `rx:use` elements will be processed as if they were
   inside a nameless `rx:provide` element.

### rx:provide

Provide a fragment of XML to be wrapped. Only used inside `rx:wrap`.

~~~~
  <rx:provide name='text'/>
~~~~

name
:  (optional) The name of the `rx:use` element to provide content for. Can
   be absent to match a nameless `rx:use` element.

### rx:use

Use a provided XML fragment. This element is essentially a place-holder.

~~~~
  <rx:use name='text'/>
~~~~

name
:  (optional) There must be no more than one nameless `rx:use` element per
   resource or resource portion (if using XPointer).

Full documentation at <http://jedahu.github.com/ring-xslt/>.
