$(document).ready(function(){

    $('.nBtn, .table .eBtn').on('click',function(event){
        event.preventDefault();
        var href=$(this).attr('href');
        var text=$(this).text();

        if (text=='Edit') {
            $.get(href, function(instrument,status){
                $('.myForm #id').val(instrument.id);
                $('.myForm #name').val(instrument.name);
                $('.myForm #shortName').val(instrument.shortName);
            });
        } else {
            $('.myForm #id').val('');
            $('.myForm #name').val('');
            $('.myForm #shortName').val('');
        }
        $('.myForm #exampleModal').modal();
    });
});
