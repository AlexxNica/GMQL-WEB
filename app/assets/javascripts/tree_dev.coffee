$ ->
  setTimeout expand, 2000
#  setTimeout selectFirstPublic, 8000
#  setTimeout runTimeout, 3000
  setTimeout expandFirstPublic, 3000
  setTimeout selectFirstFirstPublic, 4000
#  setTimeout doOnFirstFirstPublic, 5000
  setTimeout doOnFirstPrivate, 5000

#  setTimeout expand, 5000
#
#  setTimeout expand, 2000
#  setTimeout selectFirstPrivate, 4000
#  setTimeout expandFirstPrivate, 4000
#  setTimeout selectFirstFirstPrivate, 6000


expand = ->
  $("#tree").fancytree("getRootNode").visit((node)-> node.setExpanded(true))



expandFirstPublic = ->
  $("#tree").fancytree("getRootNode").children[1].children[0].setExpanded(true)

selectFirstPublic = ->
  firstDS = $("#tree").fancytree("getRootNode").children[1].children[0]
  firstDS.setActive(true)

selectFirstFirstPublic = ->
  firstDS = $("#tree").fancytree("getRootNode").children[1].children[0].children[0]
  firstDS.setActive(true)



expandFirstPrivate = ->
  $("#tree").fancytree("getRootNode").children[0].children[0].setExpanded(true)

selectFirstPrivate = ->
  firstDS = $("#tree").fancytree("getRootNode").children[0].children[0]
  firstDS.setActive(true)

selectFirstFirstPrivate = ->
  firstDS = $("#tree").fancytree("getRootNode").children[0].children[0].children[0]
  firstDS.setActive(true)



doOnFirstPrivate = ->
  node = $("#tree").fancytree("getRootNode").children[0].children[0]
  showQuery node

doOnFirstFirstPublic = ->
  node = $("#tree").fancytree("getRootNode").children[1].children[0].children[0]
  showMetaRegion node, true
