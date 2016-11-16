var selectedTiles = {};

function toggleTile(tileName) {
	$(`a[name='${tileName}']`).toggleClass('selected');
	if (tileName in selectedTiles) {
		delete selectedTiles[tileName];
	} else {
		selectedTiles[tileName] = true;
	}
	updateTextList();
}

function updateTextList() {
	var text = '';
	for (var tileName in selectedTiles) {
		text += tileName + '\n';
	}

	var tilesListEl = $('#selectedTiles');
	$('#selectedTiles textarea').val(text);
	if (text) {
		tilesListEl.show();
	} else {
		tilesListEl.hide();
	}
}

function replaceSelected(newSelectedTiles) {
	$('.selected').toggleClass('selected');
	selectedTiles = {};
	for (var i = 0; i < newSelectedTiles.length; i++) {
		var tileName = newSelectedTiles[i];
		var matchedTiles = $(`a[name='${tileName}']`);
		if (matchedTiles.length) {
			matchedTiles.toggleClass('selected');
			selectedTiles[tileName] = true;
		}
		
	}
}

$(function() {
	$('#selectedTiles textarea').on('input', function() {
		replaceSelected($(this).val().trim().split(/\s+/));
	});
});
